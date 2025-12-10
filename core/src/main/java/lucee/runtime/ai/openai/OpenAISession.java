package lucee.runtime.ai.openai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import lucee.commons.io.CharsetUtil;
import lucee.commons.io.IOUtil;
import lucee.commons.lang.ExceptionUtil;
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
import lucee.runtime.exp.ApplicationException;
import lucee.runtime.exp.PageException;
import lucee.runtime.interpreter.JSONExpressionInterpreter;
import lucee.runtime.listener.SerializationSettings;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Array;
import lucee.runtime.type.ArrayImpl;
import lucee.runtime.type.Struct;
import lucee.runtime.type.StructImpl;
import lucee.runtime.type.util.KeyConstants;

public final class OpenAISession extends AISessionSupport {

	private OpenAIEngine openaiEngine;
	private String systemMessage;

	public OpenAISession(OpenAIEngine engine, String systemMessage, Conversation[] history, int limit, double temp, int connectTimeout, int socketTimeout) {
		super(engine, history, limit, temp, connectTimeout, socketTimeout);
		this.openaiEngine = engine;
		this.systemMessage = systemMessage;

	}

	@Override
	public Response inquiry(String message, AIResponseListener listener) throws PageException {
		try {
			if (openaiEngine.chatCompletionsURI == null) openaiEngine.chatCompletionsURI = new URI(openaiEngine.getBaseURL() + "chat/completions");
			Struct msg;
			Array arr = new ArrayImpl();
			// add system
			if (!StringUtil.isEmpty(systemMessage)) {
				msg = new StructImpl(StructImpl.TYPE_LINKED);
				msg.set(KeyConstants._role, "system");
				msg.set(KeyConstants._content, systemMessage);
				arr.append(msg);
			}

			// Add conversation history
			for (Conversation c: getHistoryAsList()) {
				// question
				msg = new StructImpl(StructImpl.TYPE_LINKED);
				msg.set(KeyConstants._role, "user");
				msg.set(KeyConstants._content, c.getRequest().getQuestion());
				arr.append(msg);

				// answer
				msg = new StructImpl(StructImpl.TYPE_LINKED);
				msg.set(KeyConstants._role, "assistant");
				msg.set(KeyConstants._content, AIUtil.extractStringAnswer(c.getResponse()));
				arr.append(msg);

			}

			// Add new user messages
			msg = new StructImpl(StructImpl.TYPE_LINKED);
			msg.set(KeyConstants._role, "user");
			msg.set(KeyConstants._content, message);
			arr.append(msg);

			Struct sct = new StructImpl(StructImpl.TYPE_LINKED);
			sct.set(KeyConstants._model, openaiEngine.getModel());
			sct.set(KeyConstants._messages, arr);
			sct.set(KeyConstants._stream, listener != null);

			// Add temperature if set in engine
			Double temperature = getTemperature();
			if (temperature != null) {
				sct.set(KeyConstants._temperature, temperature);
			}

			// TODO response_format
			// TODO frequency_penalty
			// TODO logit_bias
			// TODO logprobs
			// TODO top_logprobs
			// TODO max_tokens
			// TODO presence_penalty

			JSONConverter json = new JSONConverter(true, CharsetUtil.UTF8, JSONDateFormat.PATTERN_CF, false);
			String str = json.serialize(null, sct, SerializationSettings.SERIALIZE_AS_COLUMN, null);
			try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

				// Execute the request
				CloseableHttpResponse response = null;
				try {
					response = execute(httpClient, str);

					HttpEntity responseEntity = response.getEntity();
					Header ct = responseEntity.getContentType();
					MimeType mt = MimeType.getInstance(ct.getValue());
					String t = mt.getType() + "/" + mt.getSubtype();
					String cs = mt.getCharset() != null ? mt.getCharset().toString() : openaiEngine.charset;

					// stream false
					if ("application/json".equals(t)) {
						// String cs = ct.getCharset();
						// getContent(rsp, cs);
						if (Util.isEmpty(cs, true)) cs = openaiEngine.charset;
						String rawStr = EntityUtils.toString(responseEntity, openaiEngine.charset);
						Struct raw = Caster.toStruct(new JSONExpressionInterpreter().interpret(null, rawStr));

						Struct err = Caster.toStruct(raw.get(KeyConstants._error, null), null);
						if (err != null) {

							throw AIUtil.toException(this.getEngine(), Caster.toString(err.get(KeyConstants._message)), Caster.toString(err.get(KeyConstants._type, null), null),
									Caster.toString(err.get(KeyConstants._code, null), null), response.getStatusLine().getStatusCode());
						}

						OpenAIResponse r = new OpenAIResponse(raw, cs);
						AIUtil.addConversation(this, getHistoryAsList(), new ConversationImpl(new RequestSupport(message), r));

						return r;
					}
					// stream true
					else if ("text/event-stream".equals(t)) {
						// String cs = ct.getCharset();
						if (Util.isEmpty(cs, true)) cs = openaiEngine.charset;
						JSONExpressionInterpreter interpreter = new JSONExpressionInterpreter();
						OpenAIStreamResponse r = new OpenAIStreamResponse(cs, listener);
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
						catch (Exception e) {
							throw Caster.toPageException(e);
						}
						AIUtil.addConversation(this, getHistoryAsList(), new ConversationImpl(new RequestSupport(message), r));
						return r;
					}
					else {
						throw unsupportedMimeTypeException(response, responseEntity, cs, t);
					}
				}
				finally {
					IOUtil.closeEL(response);
				}
			}
		}
		catch (SocketTimeoutException ste) {
			ApplicationException ae = new ApplicationException(
					"A socket timeout occurred while querying the AI Engine [" + openaiEngine.getLabel() + "]. The configured timeout was " + getSocketTimeout() + " ms.");
			ExceptionUtil.initCauseEL(ae, ste);
			throw ae;
		}
		catch (Exception e) {
			throw Caster.toPageException(e);
		}
	}

	private CloseableHttpResponse execute(CloseableHttpClient httpClient, String str) throws ClientProtocolException, IOException, URISyntaxException {
		int max = 3;
		CloseableHttpResponse response = httpClient.execute(createHttpPost(openaiEngine.chatCompletionsURI, str));
		while (response.getStatusLine().getStatusCode() >= 300 && response.getStatusLine().getStatusCode() < 400) {
			if (--max == 0) return response;
			// Get the Location header
			Header locationHeader = response.getFirstHeader("Location");
			if (locationHeader != null) {
				String redirectUrl = locationHeader.getValue();
				IOUtil.closeEL(response);

				openaiEngine.chatCompletionsURI = new URI(

						openaiEngine.chatCompletionsURI.getScheme(),

						openaiEngine.chatCompletionsURI.getUserInfo(),

						openaiEngine.chatCompletionsURI.getHost(),

						openaiEngine.chatCompletionsURI.getPort(), redirectUrl,

						openaiEngine.chatCompletionsURI.getQuery(),

						openaiEngine.chatCompletionsURI.getFragment());

				response = httpClient.execute(createHttpPost(openaiEngine.chatCompletionsURI, str));
			}
		}
		return response;
	}

	private HttpPost createHttpPost(URI uri, String str) {
		HttpPost post = new HttpPost(uri);
		post.setHeader("Content-Type", AIUtil.createJsonContentType(openaiEngine.charset));
		post.setHeader("Authorization", "Bearer " + openaiEngine.secretKey);

		StringEntity entity = new StringEntity(str, openaiEngine.charset);
		post.setEntity(entity);

		RequestConfig config = AISessionSupport.setRedirect(AISessionSupport.setTimeout(RequestConfig.custom(), this)).build();
		post.setConfig(config);

		return post;
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
