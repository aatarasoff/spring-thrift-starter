package info.developerblog.spring.thrift.sleuth;

import info.developerblog.spring.thrift.transport.TLoadBalancerClient;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanInjector;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by aleksandr on 27.06.16.
 */
public class ThriftHttpTransportSpanInjector implements SpanInjector<TTransport> {
    @Override
    public void inject(Span span, TTransport carrier) {
        if (carrier instanceof THttpClient) {
            ((THttpClient)carrier).setCustomHeaders(sleuthHeaders(span));
        } else {
            ((TLoadBalancerClient)carrier).setCustomHeaders(sleuthHeaders(span));
        }
    }

    private Map<String, String> sleuthHeaders(Span span) {
        Map<String, String> headers = new HashMap<>();
        if (span == null) {
            headers.put(Span.SAMPLED_NAME, Span.SPAN_NOT_SAMPLED);
        } else {
            headers.put(Span.TRACE_ID_NAME, Span.idToHex(span.getTraceId()));
            headers.put(Span.SPAN_NAME_NAME, span.getName());
            headers.put(Span.SPAN_ID_NAME, Span.idToHex(span.getSpanId()));
            headers.put(Span.SAMPLED_NAME, span.isExportable() ?
                    Span.SPAN_SAMPLED : Span.SPAN_NOT_SAMPLED);
            Long parentId = getParentId(span);
            if (parentId != null) {
                headers.put(Span.PARENT_ID_NAME, Span.idToHex(parentId));
            }
            headers.put(Span.PROCESS_ID_NAME, span.getProcessId());
        }
        return headers;
    }

    private Long getParentId(Span span) {
        return !span.getParents().isEmpty() ? span.getParents().get(0) : null;
    }
}
