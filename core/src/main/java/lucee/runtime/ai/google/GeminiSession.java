package lucee.runtime.ai.google;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import lucee.commons.io.CharsetUtil;
import lucee.commons.lang.StringUtil;
import lucee.commons.lang.mimetype.MimeType;
import lucee.loader.util.Util;
import lucee.runtime.ai.AIResponseListener;
import lucee.runtime.ai.AISessionSupport;
import lucee.runtime.ai.AIUtil;
import lucee.runtime.ai.Conversation;
import lucee.runtime.ai.ConversationImpl;
import lucee.runtime.ai.RequestSupport;
import lucee.runtime.ai.Response;
import lucee.runtime.converter.JSONConverter;
import lucee.runtime.converter.JSONDateFormat;
import lucee.runtime.exp.PageException;
import lucee.runtime.interpreter.JSONExpressionInterpreter;
import lucee.runtime.listener.SerializationSettings;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Array;
import lucee.runtime.type.ArrayImpl;
import lucee.runtime.type.Struct;
import lucee.runtime.type.StructImpl;
import lucee.runtime.type.util.KeyConstants;

public final class GeminiSession extends AISessionSupport {
	private GeminiEngine geminiEngine;
	private String systemMessage;

	public GeminiSession(GeminiEngine engine, String systemMessage, Conversation[] history, int limit, double temp, int connectTimeout, int socketTimeout) {
		super(engine, history, limit, temp, connectTimeout, socketTimeout);
		this.geminiEngine = engine;
		this.systemMessage = systemMessage;
	}

	@Override
	public Response inquiry(String message, AIResponseListener listener) throws PageException {
		try {
			Struct root = new StructImpl(StructImpl.TYPE_LINKED);
			Array contents = new ArrayImpl();
			root.set(KeyConstants._contents, contents);

			// Add temperature if set in engine
			Double temperature = getTemperature();
			if (temperature != null) {
				Struct generationConfig = new StructImpl();
				generationConfig.set("temperature", temperature);
				root.set("generationConfig", generationConfig);
			}

			if (!StringUtil.isEmpty(systemMessage, true)) {
				contents.append(createParts("user", systemMessage));
			}

			for (Conversation c: getHistoryAsList()) {
				contents.append(createParts("user", c.getRequest().getQuestion()));
				contents.append(createParts("model", AIUtil.extractStringAnswer(c.getResponse())));
			}

			contents.append(createParts("user", message));

			HttpPost post = new HttpPost(
					geminiEngine.toURL(geminiEngine.baseURL, GeminiEngine.CHAT, listener != null ? GeminiEngine.TYPE_STREAM : GeminiEngine.TYPE_REG).toExternalForm());
			post.setHeader("Content-Type", AIUtil.createJsonContentType(geminiEngine.charset));

			JSONConverter json = new JSONConverter(true, CharsetUtil.UTF8, JSONDateFormat.PATTERN_CF, false);
			String str = json.serialize(null, root, SerializationSettings.SERIALIZE_AS_COLUMN, null);
			StringEntity entity = new StringEntity(str, geminiEngine.charset);
			post.setEntity(entity);

			RequestConfig config = AISessionSupport.setTimeout(RequestConfig.custom(), this).build();
			post.setConfig(config);

			try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(post)) {

				HttpEntity responseEntity = response.getEntity();
				Header ct = responseEntity.getContentType();
				MimeType mt = MimeType.getInstance(ct.getValue());

				String t = mt.getType() + "/" + mt.getSubtype();
				String cs = mt.getCharset() != null ? mt.getCharset().toString() : geminiEngine.charset;

				if ("text/event-stream".equals(t)) {
					if (Util.isEmpty(cs, true)) cs = geminiEngine.charset;
					JSONExpressionInterpreter interpreter = new JSONExpressionInterpreter();
					GeminiStreamResponse r = new GeminiStreamResponse(cs, listener);

					try (BufferedReader reader = new BufferedReader(
							cs == null ? new InputStreamReader(responseEntity.getContent()) : new InputStreamReader(responseEntity.getContent(), cs))) {
						String line;
						int index = 0;
						Struct prev = null;
						while ((line = reader.readLine()) != null) {
							if (prev != null) {
								r.addPart(prev, index++, false);
								prev = null;
							}

							if (!line.startsWith("data: ")) continue;
							line = line.substring(6);
							prev = Caster.toStruct(interpreter.interpret(null, line));
						}
						if (prev != null) {
							r.addPart(prev, index, true);
						}
					}

					AIUtil.addConversation(this, getHistoryAsList(), new ConversationImpl(new RequestSupport(message), r));
					return r;
				}
				else if ("application/json".equals(t)) {
					if (Util.isEmpty(cs, true)) cs = geminiEngine.charset;
					String rawStr = EntityUtils.toString(responseEntity, geminiEngine.charset);
					Struct raw = Caster.toStruct(new JSONExpressionInterpreter().interpret(null, rawStr));

					Struct err = Caster.toStruct(raw.get(KeyConstants._error, null), null);
					if (err != null) {
						throw AIUtil.toException(this.getEngine(), Caster.toString(err.get(KeyConstants._message)), Caster.toString(err.get(KeyConstants._status, null), null),
								Caster.toString(err.get(KeyConstants._code, null), null), AIUtil.getStatusCode(response));
					}

					GeminiResponse r = new GeminiResponse(raw, cs);
					AIUtil.addConversation(this, getHistoryAsList(), new ConversationImpl(new RequestSupport(message), r));
					return r;
				}
				else {
					throw unsupportedMimeTypeException(response, responseEntity, cs, t);
				}
			}
		}
		catch (Exception e) {
			throw Caster.toPageException(e);
		}
	}

	private Struct createParts(String role, String msg) throws PageException {
		Struct sctContents = new StructImpl(StructImpl.TYPE_LINKED);

		Array parts = new ArrayImpl();
		Struct sct = new StructImpl(StructImpl.TYPE_LINKED);
		parts.append(sct);
		sct.set(KeyConstants._text, msg);

		if (role != null) sctContents.set(KeyConstants._role, role.trim());
		sctContents.set(KeyConstants._parts, parts);

		return sctContents;
	}

	@Override
	public void release() {
		// nothing to give up
	}

	@Override
	public String getSystemMessage() {
		return systemMessage;
	}

}
