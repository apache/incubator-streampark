export interface HadoopConf {
  hadoop: Hadoop;
  hive: any;
}

interface Hadoop {
  'core-site.xml': string;
  'hdfs-site.xml': string;
  'yarn-site.xml': string;
}
