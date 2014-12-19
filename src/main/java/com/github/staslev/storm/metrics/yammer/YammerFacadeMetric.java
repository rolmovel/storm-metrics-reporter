package com.github.staslev.storm.metrics.yammer;

import backtype.storm.metric.api.IMetric;
import backtype.storm.task.TopologyContext;
import com.google.common.collect.ImmutableMap;
import com.yammer.metrics.core.*;
import com.yammer.metrics.stats.Snapshot;

import java.util.HashMap;
import java.util.Map;

public class YammerFacadeMetric implements IMetric, MetricProcessor<Map> {

  public static final String FACADE_METRIC_TIME_BUCKET_IN_SEC = "metrics.reporter.yammer.facade..metric.bucket.seconds";
  public static final String FACADE_METRIC_NAME = "YammerFacadeMetric";

  private final MetricsRegistry metricsRegistry;

  private YammerFacadeMetric(final MetricsRegistry metricsRegistry) {
    this.metricsRegistry = metricsRegistry;
  }

  public static void register(final Map stormConf,
                              final TopologyContext context,
                              final MetricsRegistry metricsRegistry) {

    context.registerMetric(FACADE_METRIC_NAME,
                           new YammerFacadeMetric(metricsRegistry),
                           Integer.parseInt(stormConf.get(FACADE_METRIC_TIME_BUCKET_IN_SEC).toString()));
  }

  @SuppressWarnings("unchecked")
  @Override
  public void processMeter(final MetricName name, final Metered meter, final Map context) throws Exception {

    final Map subMetrics =
            ImmutableMap
                    .builder()
                    .put("count", meter.count())
                    .put("meanRate", meter.meanRate())
                    .put("1MinuteRate", meter.oneMinuteRate())
                    .put("5MinuteRate", meter.fiveMinuteRate())
                    .put("15MinuteRate", meter.fifteenMinuteRate())
                    .build();

    context.put(name.getName(), subMetrics);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void processCounter(final MetricName name, final Counter counter, final Map context) throws Exception {
    context.put(name.getName(), counter.count());
  }

  @SuppressWarnings("unchecked")
  @Override
  public void processHistogram(final MetricName name, final Histogram histogram, final Map context) throws Exception {

    final Snapshot snapshot = histogram.getSnapshot();

    final Map subMetrics =
            ImmutableMap
                    .builder()
                    .put("75percentile", snapshot.get75thPercentile())
                    .put("95percentile", snapshot.get95thPercentile())
                    .put("99percentile", snapshot.get99thPercentile())
                    .put("median", snapshot.getMedian())
                    .put("mean", histogram.mean())
                    .put("min", histogram.min())
                    .put("max", histogram.max())
                    .put("stddev", histogram.stdDev())
                    .build();


    context.put(name.getName(), subMetrics);
  }


  @SuppressWarnings("unchecked")
  @Override
  public void processTimer(final MetricName name, final Timer timer, final Map context) throws Exception {

    final Snapshot snapshot = timer.getSnapshot();

    final Map subMetrics =
            ImmutableMap
                    .builder()
                    .put("count", timer.count())
                    .put("median", snapshot.getMedian())
                    .put("75percentile", snapshot.get75thPercentile())
                    .put("95percentile", snapshot.get95thPercentile())
                    .put("99percentile", snapshot.get99thPercentile())
                    .build();

    context.put(name.getName(), subMetrics);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void processGauge(final MetricName name,
                           final com.yammer.metrics.core.Gauge<?> gauge,
                           final Map context) throws Exception {
    context.put(name.getName(), gauge.value());
  }

  @Override
  public Object getValueAndReset() {

    final Map metricsValues = new HashMap();

    for (final Map.Entry<MetricName, Metric> entry : metricsRegistry.allMetrics().entrySet()) {
      try {
        entry.getValue().processWith(this, entry.getKey(), metricsValues);
      } catch (final Exception e) {
        // log?
      }
    }

    return metricsValues;
  }
}
