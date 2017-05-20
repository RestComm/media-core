package org.mobicents.media.server.tts.voicerss;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.mobicents.media.server.tts.core.SpeechSynthesizerException;
import org.mobicents.media.server.tts.core.TTSEngine;


/**
 * VoiceRssEngine contains all the logic to parse the configuration file and processing text data
 * 
 * @author Thanh Tran
 *
 */
public class VoiceRssEngine implements TTSEngine{
	private final static Logger logger = Logger.getLogger(VoiceRssEngine.class);
	private final static int BUFFER = 1024 * 8;
	private static final List<NameValuePair> parameters;
	static {
		parameters = new ArrayList<NameValuePair>();
		parameters.add(new BasicNameValuePair("c", "WAV"));
		parameters.add(new BasicNameValuePair("f", "8khz_16bit_mono"));
	}
	
	// Define elements of configuration xml file
	private final static String 	SERVICE_ROOT = "service-root";
	private final static String 	API_KEY = "apikey";
	
	// Configuration xml elements data
	private String 					myAPIKey;
	
	private final URI myURIService;
	private final Map<String, String> myMapLanguage;
	
	public VoiceRssEngine(final String configurationPath) throws ConfigurationException {
		if ( logger.isInfoEnabled() ) {
			logger.info("Configuration file: " + configurationPath);
		}
		
		myMapLanguage = new HashMap<String, String>();
		
		final URL url = getClass().getResource(configurationPath);
		final XMLConfiguration configuration = new XMLConfiguration(url);

		myAPIKey = configuration.getString(API_KEY);
		BasicNameValuePair apiNameValuePair = new BasicNameValuePair("key", myAPIKey);
		if ( !parameters.contains(apiNameValuePair) ) {
			parameters.add(apiNameValuePair);
		} else {
			// Do nothing
		}
		
		// Initialize the speech synthesizer state.
		myURIService = URI.create(configuration.getString(SERVICE_ROOT));
		load(configuration);
	}
	
	@Override
	public byte[] speak(String text) {
		byte[] speakData = new byte[BUFFER];
		List<NameValuePair> query = new ArrayList<NameValuePair>();
		query.addAll(parameters);
		//query.add(new BasicNameValuePair("hl", getLanguage(language)));
		query.add(new BasicNameValuePair("src", text));
		
		final HttpPost post = new HttpPost(myURIService);
		try {
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(query, "UTF-8");
			post.setEntity(entity);
			final HttpClient client = new DefaultHttpClient();
			HttpResponse response = client.execute(post);
			final StatusLine line = response.getStatusLine();

			if ( HttpStatus.SC_OK == line.getStatusCode() ) {
				Header[] contentType = response.getHeaders("Content-Type");
				
				if ( contentType[0].getValue().startsWith("text") ) {
					final StringBuilder buffer = new StringBuilder();
					String error = EntityUtils.toString(response.getEntity());
					logger.error("VoiceRssEngine error: " + error);
					buffer.append(error);
					throw new SpeechSynthesizerException(buffer.toString());
				}
				
				if ( logger.isInfoEnabled() ) {
					logger.info("VoiceRssEngine success!");
				}
				
				InputStream is = response.getEntity().getContent();
				while (true) {
					try {
						int len = is.read(speakData);
						if ( len <= 0 ) {
							break;
						}
						is.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else {
				if ( logger.isInfoEnabled() ) {
					logger.info("VoiceRssEngine error, status code: " + line.getStatusCode() + (" reason phrase: ") + line.getReasonPhrase());
				} else {
					// Do nothing
				}
				final StringBuilder buffer = new StringBuilder();
				buffer.append(line.getStatusCode()).append(" ").append(line.getReasonPhrase());
				throw new SpeechSynthesizerException(buffer.toString());
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return speakData;
	}
	
	private void load(final Configuration configuration) throws RuntimeException {
		// Initialize
		myMapLanguage.put("ca", configuration.getString("languages.catalan"));
		myMapLanguage.put("zh-cn", configuration.getString("languages.chinese-china"));
		myMapLanguage.put("zh-hk", configuration.getString("languages.chinese-hongkong"));
		myMapLanguage.put("zh-tw", configuration.getString("languages.chinese-taiwan"));
		myMapLanguage.put("da", configuration.getString("languages.danish"));
		myMapLanguage.put("nl", configuration.getString("languages.dutch"));
		myMapLanguage.put("en-au", configuration.getString("languages.english-australia"));
		myMapLanguage.put("en-ca", configuration.getString("languages.english-canada"));
		myMapLanguage.put("en-gb", configuration.getString("languages.english-greatbritain"));
		myMapLanguage.put("en-in", configuration.getString("languages.english-india"));
		myMapLanguage.put("en", configuration.getString("languages.english-us"));
		myMapLanguage.put("fi", configuration.getString("languages.finish"));
		myMapLanguage.put("fr-ca", configuration.getString("languages.french-canada"));
		myMapLanguage.put("fr", configuration.getString("languages.french-france"));
		myMapLanguage.put("de", configuration.getString("languages.german"));
		myMapLanguage.put("it", configuration.getString("languages.italina"));
		myMapLanguage.put("ja", configuration.getString("languages.japanese"));
		myMapLanguage.put("ko", configuration.getString("languages.korean"));
		myMapLanguage.put("nb", configuration.getString("languages.norwegian"));
		myMapLanguage.put("pl", configuration.getString("languages.polish"));
		myMapLanguage.put("pt-br", configuration.getString("languages.portuguese-brasil"));
		myMapLanguage.put("pt", configuration.getString("languages.portuguese-portugal"));
		myMapLanguage.put("ru", configuration.getString("languages.russian"));
		myMapLanguage.put("es-mx", configuration.getString("languages.spanish-mexico"));
		myMapLanguage.put("es", configuration.getString("languages.spanish-spain"));
		myMapLanguage.put("sv", configuration.getString("languages.swedish"));
	}

	public String getAPIkey() {
		if ( myAPIKey.isEmpty() ) {
			logger.warn("API Key is empty");
		} else {
			// Do nothing
		}
		return myAPIKey;
	}
	
	public String getLanguage(final String language) {
		String languageCode = myMapLanguage.get(language);
		return languageCode;
	}

	public static String hashMessage(String languare, String text) {
		return new Sha256Hash(languare + text).toHex();
	}
}
