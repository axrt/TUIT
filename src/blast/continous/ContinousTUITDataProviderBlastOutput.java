package blast.continous;

import java.nio.file.Path;

/**
 * Created by alext on 1/22/15.
 */
public interface ContinousTUITDataProviderBlastOutput extends ContinousTUITDataProvider {

    public Path toBlastOutput() throws Exception;

}
