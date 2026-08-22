package common;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Set;

public class JSONUtil {

    public static boolean isSameJsonObject(JSONObject first,JSONObject second)
    {
        Set<String> firstKeySet = first.keySet();
        Set<String> secondKeySet = second.keySet();
        if(firstKeySet.size()!= secondKeySet.size())
        {
            return false;
        }

        for(String key : firstKeySet)
        {
            if(!secondKeySet.contains(key) || !first.get(key).equals(second.get(key)))
            {
                return false;
            }
        }

        for(String key : secondKeySet)
        {
            if(!firstKeySet.contains(key) || !first.get(key).equals(second.get(key)))
            {
                return false;
            }
        }

        return true;
    }
}
