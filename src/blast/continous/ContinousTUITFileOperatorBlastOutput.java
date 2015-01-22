package blast.continous;

import io.file.TUITFileOperatorHelper;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by alext on 1/22/15.
 */
public class ContinousTUITFileOperatorBlastOutput extends ContinousTUITFileOperator implements ContinousTUITDataProviderBlastOutput{

    protected final Path toBlastOutput;

    protected ContinousTUITFileOperatorBlastOutput(Path executable, Path query, Path output, TUITFileOperatorHelper.OutputFormat format, Path toBlastOutput) throws IOException {
        super(executable, query, output, format);
        this.toBlastOutput=toBlastOutput;
    }

    @Override
    public Path toBlastOutput() throws Exception {
        return this.toBlastOutput;
    }

    public static ContinousTUITFileOperatorBlastOutput get(Path executable, Path query, Path output, TUITFileOperatorHelper.OutputFormat format, Path toBlastOutput) throws IOException {
        return new ContinousTUITFileOperatorBlastOutput(executable,query,output,format,toBlastOutput);
    }
}
