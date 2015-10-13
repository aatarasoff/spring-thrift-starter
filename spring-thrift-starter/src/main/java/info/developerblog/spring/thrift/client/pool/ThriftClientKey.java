package info.developerblog.spring.thrift.client.pool;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
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
        if (Strings.isNullOrEmpty(serviceName))
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
