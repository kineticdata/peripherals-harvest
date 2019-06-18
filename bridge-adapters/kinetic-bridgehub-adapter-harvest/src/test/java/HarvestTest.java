import com.kineticdata.bridgehub.adapter.BridgeAdapter;
import com.kineticdata.bridgehub.adapter.BridgeAdapterTestBase;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.harvest.HarvestAdapter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HarvestTest extends BridgeAdapterTestBase{
        
    @Override
    public Class getAdapterClass() {
        return HarvestAdapter.class;
    }
    
    @Override
    public String getConfigFilePath() {
        return "src/test/resources/bridge-config.yml";
    }
    
    @Test
    public void testCount() throws Exception{
        BridgeError error = null;
        
        Map<String,String> configValues = new LinkedHashMap<String,String>();
        configValues.put("Username","admin");
        configValues.put("Password", "admin");
        configValues.put("Your Harvest App Account","kineticdata");
        
        BridgeAdapter adapter = new HarvestAdapter();
        adapter.setProperties(configValues);
        try {
            adapter.initialize();
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        
        // Create the Bridge Request
        List<String> fields = new ArrayList<String>();
        fields.add("id");
        
        BridgeRequest request = new BridgeRequest();
        request.setStructure("Projects");
        request.setFields(fields);
        request.setQuery("");
        
        Count count = null;
        try {
            count = adapter.count(request);
        } catch (BridgeError e) {
            error = e;
        }
        
        assertNull(error);
        assertTrue(count.getValue() > 0);
    }
}
