package lucee.runtime.search;

import java.util.Map;

public interface SearchDataPro extends SearchData {

	public void setAddionalAttribute( String name, Object value );

	public Map<String, Object> getAddionalAttributes();
}
