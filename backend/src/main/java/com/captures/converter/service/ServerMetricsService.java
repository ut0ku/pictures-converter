package com.captures.converter.service;

import com.captures.converter.dto.Dtos.ServerMetrics;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;

@Service
public class ServerMetricsService {
 public ServerMetrics read(){
  Double systemCpuLoad=null;
  Double processCpuLoad=null;
  Long systemMemoryTotalBytes=null;
  Long systemMemoryUsedBytes=null;
  java.lang.management.OperatingSystemMXBean operatingSystem=ManagementFactory.getOperatingSystemMXBean();

  if(operatingSystem instanceof com.sun.management.OperatingSystemMXBean extended){
   systemCpuLoad=normalizeLoad(extended.getCpuLoad());
   processCpuLoad=normalizeLoad(extended.getProcessCpuLoad());
   long total=extended.getTotalMemorySize();
   long free=extended.getFreeMemorySize();
   if(total>0&&free>=0){
    systemMemoryTotalBytes=total;
    systemMemoryUsedBytes=total-Math.min(total,free);
   }
  }

  Runtime runtime=Runtime.getRuntime();
  long heapUsed=runtime.totalMemory()-runtime.freeMemory();
  return new ServerMetrics(
    systemCpuLoad,
    processCpuLoad,
    heapUsed,
    runtime.maxMemory(),
    systemMemoryUsedBytes,
    systemMemoryTotalBytes,
    ManagementFactory.getRuntimeMXBean().getUptime(),
    ManagementFactory.getThreadMXBean().getThreadCount(),
    runtime.availableProcessors(),
    System.getProperty("java.version"),
    operatingSystem.getName()
  );
 }

 private Double normalizeLoad(double load){
  return Double.isFinite(load)&&load>=0?Math.min(load,1):null;
 }
}
