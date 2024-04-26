package eu.unicore.services.aip.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface IFileParser {

	public Map<String, List<Attribute>> parse(InputStream is) throws IOException;

}
