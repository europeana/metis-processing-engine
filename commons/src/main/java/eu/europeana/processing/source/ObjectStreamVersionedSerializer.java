package eu.europeana.processing.source;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.apache.flink.core.io.SimpleVersionedSerializer;

/**
 * SimpleVersionedSerializer implementation using java ObjectOutputStream and ObjectInputStream.
 *
 * @param <T> type of the serialized object.
 */
public class ObjectStreamVersionedSerializer<T extends Serializable> implements SimpleVersionedSerializer<T> {

  @Override
  public int getVersion() {
    return 0;
  }

  @Override
  public byte[] serialize(T obj) throws IOException {
    var boas = new ByteArrayOutputStream();
    var oos = new ObjectOutputStream(boas);
    oos.writeObject(obj);

    return boas.toByteArray();
  }

  @Override
  public T deserialize(int version, byte[] serialized) throws IOException {
    var bais = new ByteArrayInputStream(serialized);
    var ois = new ObjectInputStream(bais);
    T res = null;
    try {
      res = (T) ois.readObject();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    ois.close();
    bais.close();
    return res;
  }
}
