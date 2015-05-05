/*
 *    SAS4J
 *
 *    Hugo Zaragoza, Websays.
 */
package websays.core.utils.jodatime;

import java.lang.reflect.Type;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * From: http://stackoverflow.com/questions/26287074/serializing-custom-object-that-contains-jodatime-objects-into-json
 * 
 * @author hugoz
 *
 */
public class LocalDateSerializer implements JsonDeserializer<LocalDate>, JsonSerializer<LocalDate> {
  
  private static final DateTimeFormatter DATE_FORMAT = ISODateTimeFormat.date();
  
  @Override
  public LocalDate deserialize(JsonElement je, final Type type, final JsonDeserializationContext jdc) throws JsonParseException {
    final String dateAsString = je.getAsString();
    if (dateAsString.length() == 0) {
      return null;
    } else {
      return DATE_FORMAT.parseLocalDate(dateAsString);
    }
  }
  
  @Override
  public JsonElement serialize(LocalDate src, final Type typeOfSrc, final JsonSerializationContext context) {
    String retVal;
    if (src == null) {
      retVal = "";
    } else {
      retVal = DATE_FORMAT.print(src);
    }
    return new JsonPrimitive(retVal);
  }
  
}