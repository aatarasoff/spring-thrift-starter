package info.developerblog.spring.thrift.client.pool;

import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.thrift.TServiceClient;

/**
 * Created by aleksandr on 11.10.15.
 */
@EqualsAndHashCode
public class ThriftClientKey {
    private Class<? extends TServiceClient> clazz;
    private String serviceName;

    public ThriftClientKey(Class<? extends TServiceClient> clazz, String serviceName) {
        this.clazz = clazz;
        this.serviceName = serviceName;
    }

    public Class<? extends TServiceClient> getClazz() {
        return clazz;
    }

    public String getServiceName() {
        if (Strings.isNullOrEmpty(serviceName))
            return WordUtils.uncapitalize(clazz.getEnclosingClass().getSimpleName());
        return serviceName;
    }
}
