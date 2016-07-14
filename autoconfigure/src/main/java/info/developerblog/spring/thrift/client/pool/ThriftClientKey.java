package info.developerblog.spring.thrift.client.pool;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.thrift.TServiceClient;

/**
 * Created by aleksandr on 11.10.15.
 */
@Builder
@EqualsAndHashCode
public class ThriftClientKey {
    private Class<? extends TServiceClient> clazz;
    private String serviceName;
    private String path;

    public String getServiceName() {
        if (StringUtils.isEmpty(serviceName))
            return WordUtils.uncapitalize(clazz.getEnclosingClass().getSimpleName());
        return serviceName;
    }

    public Class<? extends TServiceClient> getClazz() {
        return clazz;
    }

    public String getPath() {
        return path;
    }
}
