package com.captures.converter;

import com.captures.converter.service.ServerMetricsService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerMetricsServiceTests {
 @Test void reportsJvmAndHostMetrics(){
  var metrics=new ServerMetricsService().read();

  assertTrue(metrics.heapUsedBytes()>=0);
  assertTrue(metrics.heapMaxBytes()>0);
  assertTrue(metrics.uptimeMillis()>=0);
  assertTrue(metrics.threadCount()>0);
  assertTrue(metrics.availableProcessors()>0);
  assertNotNull(metrics.javaVersion());
  assertNotNull(metrics.osName());
  if(metrics.systemCpuLoad()!=null)assertTrue(metrics.systemCpuLoad()>=0&&metrics.systemCpuLoad()<=1);
  if(metrics.processCpuLoad()!=null)assertTrue(metrics.processCpuLoad()>=0&&metrics.processCpuLoad()<=1);
  if(metrics.systemMemoryTotalBytes()!=null){
   assertTrue(metrics.systemMemoryTotalBytes()>0);
   assertNotNull(metrics.systemMemoryUsedBytes());
   assertTrue(metrics.systemMemoryUsedBytes()>=0&&metrics.systemMemoryUsedBytes()<=metrics.systemMemoryTotalBytes());
  }
 }
}
