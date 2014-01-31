package org.jboss.weld.invokedynamic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Antoine Sabot-Durand
 */
public class FieldMetaData {

    private final String name;
    private final Set<String> annotations;

    public FieldMetaData(String name, Set<String> annotations) {
        this.name = name;
        this.annotations = annotations;
    }

    public String getName() {
        return name;
    }

    public Set<String> getAnnotations() {
        return annotations;
    }

    public Object[] asArray()
    {
        List<String> res = new ArrayList<>(annotations);
        res.add(0,name);
        return res.toArray();
    }
}
