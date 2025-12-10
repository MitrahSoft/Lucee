// Session implementation
package lucee.runtime.ai.anthropic;

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

public final class ClaudeSession extends AISessionSupport {
	private ClaudeEngine engine;
	private String systemMessage;

	public ClaudeSession(ClaudeEngine engine, String systemMessage, Conversation[] history, int limit, double temp, int connectTimeout, int socketTimeout) {
		super(engine, history, limit, temp, connectTimeout, socketTimeout);
		this.engine = engine;
		this.systemMessage = systemMessage;
	}

	@Override
	public Response inquiry(String message, AIResponseListener listener) throws PageException {
		try {

			Struct requestBody = new StructImpl();
			requestBody.set(KeyConstants._model, engine.getModel());
			requestBody.set("max_tokens", 4096);
			requestBody.set("stream", listener != null);

			// Add temperature if set in engine
			Double temperature = getTemperature();
			if (temperature != null) {
				requestBody.set("temperature", temperature);
			}

			// Set system message at top level if exists
			if (!StringUtil.isEmpty(systemMessage)) {
				requestBody.set(KeyConstants._system, systemMessage);
			}

			// Build messages array with system and conversation history
			Array messages = new ArrayImpl();

			// Add conversation history
			for (Conversation c: getHistoryAsList()) {
				messages.append(createMessage("user", c.getRequest().getQuestion()));
				messages.append(createMessage("assistant", AIUtil.extractStringAnswer(c.getResponse())));
			}

			// Add new message
			messages.append(createMessage("user", message));
			requestBody.set("messages", messages);

			// Make API request
			HttpPost post = new HttpPost(engine.getBaseURL().toExternalForm() + "messages");
			post.setHeader("Content-Type", "application/json");
			post.setHeader("x-api-key", engine.getApiKey());
			post.setHeader("anthropic-version", engine.getVersion());

			// Convert request body to JSON
			JSONConverter json = new JSONConverter(true, CharsetUtil.UTF8, JSONDateFormat.PATTERN_CF, false);
			String str = json.serialize(null, requestBody, SerializationSettings.SERIALIZE_AS_COLUMN, null);
			// Create entity and set it to the post request
			StringEntity entity = new StringEntity(str, engine.getCharset());
			post.setEntity(entity);

			// Set timeout
			RequestConfig config = AISessionSupport.setTimeout(RequestConfig.custom(), this).build();
			post.setConfig(config);

			// Execute request
			try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(post)) {

				HttpEntity responseEntity = response.getEntity();
				Header ct = responseEntity.getContentType();
				MimeType mt = MimeType.getInstance(ct.getValue());

				String t = mt.getType() + "/" + mt.getSubtype();
				String cs = mt.getCharset() != null ? mt.getCharset().toString() : engine.getCharset();
				// Handle JSON response
				if ("application/json".equals(t)) {
					if (Util.isEmpty(cs, true)) cs = engine.getCharset();
					String rawStr = EntityUtils.toString(responseEntity, engine.getCharset());

					Struct raw = Caster.toStruct(new JSONExpressionInterpreter().interpret(null, rawStr));

					// Check for errors
					Struct err = Caster.toStruct(raw.get(KeyConstants._error, null), null);
					if (err != null) {
						throw AIUtil.toException(this.getEngine(), Caster.toString(err.get(KeyConstants._message)), Caster.toString(err.get(KeyConstants._type, null), null),
								Caster.toString(err.get(KeyConstants._code, null), null), AIUtil.getStatusCode(response));
					}

					// Create response object
					ClaudeResponse r = new ClaudeResponse(raw, cs);
					AIUtil.addConversation(this, getHistoryAsList(), new ConversationImpl(new RequestSupport(message), r));

					return r;
				}
				// Handle streaming response if needed
				else if ("text/event-stream".equals(t)) {
					if (Util.isEmpty(cs, true)) cs = engine.getCharset();
					JSONExpressionInterpreter interpreter = new JSONExpressionInterpreter();
					ClaudeStreamResponse r = new ClaudeStreamResponse(cs, listener);

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
							if ("[DONE]".equals(line)) break;
							prev = Caster.toStruct(interpreter.interpret(null, line));
						}
						if (prev != null) {
							r.addPart(prev, index, true);
						}
					}

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

	private Struct createMessage(String role, String content) {
		Struct message = new StructImpl();
		message.setEL(KeyConstants._role, role);
		message.setEL(KeyConstants._content, content);
		return message;
	}

	@Override
	public void release() throws PageException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getSystemMessage() {
		return systemMessage;
	}
}