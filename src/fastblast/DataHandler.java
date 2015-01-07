package fastblast;

/**
 * Created by alext on 1/6/15.
 */
public interface DataHandler<T> {

    public void handle(T t)throws Exception;
}
