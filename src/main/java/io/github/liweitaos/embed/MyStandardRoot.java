package io.github.liweitaos.embed;

import org.apache.catalina.Host;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.webresources.*;

import java.io.File;

public class MyStandardRoot extends StandardRoot {

    @Override
    protected WebResourceSet createMainResourceSet() {
        String docBase = getContext().getDocBase();

        WebResourceSet mainResourceSet;
        if (docBase == null) {
            mainResourceSet = new EmptyResourceSet(this);
        } else {
            File f = new File(docBase);
            if (!f.isAbsolute()) {
                f = new File(((Host) getContext().getParent()).getAppBaseFile(), f.getPath());
            }
            if (f.isDirectory()) {
                mainResourceSet = new DirResourceSet(this, "/", f.getAbsolutePath(), "/");
            } else if (f.isFile() && docBase.endsWith(".war")) {
                mainResourceSet = new WarResourceSet(this, "/", f.getAbsolutePath());
            } else if (f.isFile() && docBase.endsWith(".jar")) {
                mainResourceSet = new JarResourceSet(this, "/", f.getAbsolutePath(), "/");
            } else {
                throw new IllegalArgumentException(sm.getString("standardRoot.startInvalidMain", f.getAbsolutePath()));
            }
        }

        return mainResourceSet;
    }

}
