package milasoft.bankseller.prices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

import org.dreambot.api.methods.MethodContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Prices {
	
	BufferedReader reader = null;
	InputStream inputStream = null;
	JSONParser parser = null;
	JSONObject priceList = null;
	
	public Prices() {
		updatePrices();
	}
	
	private void updatePrices() {
		try {
			parser = new JSONParser();
			inputStream = new URL("https://rsbuddy.com/exchange/summary.json").openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
			priceList = (JSONObject) parser.parse(reader);
			reader.close();
			inputStream.close();
		} catch (IOException | ParseException e) {
			MethodContext.log("Error parsing item prices.");
			e.printStackTrace();
		}
	}
	
	public long getPrice(int itemId) {
		JSONObject item = (JSONObject) priceList.get(String.valueOf(itemId));
		if(item != null) {
			return (long) item.get("overall_average");
		} else {
			return 0;
		}
	}
}
