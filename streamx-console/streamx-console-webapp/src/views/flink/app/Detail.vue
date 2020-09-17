<template>
  <a-card :bordered="false" style="margin-top: 20px;">
    <div>
      <a-descriptions bordered title="基础信息" size="middle">
        <a-descriptions-item label="Application">
          Cloud Database
        </a-descriptions-item>
        <a-descriptions-item label="Project">
          Prepaid
        </a-descriptions-item>
        <a-descriptions-item label="StartTime">
          YES
        </a-descriptions-item>
        <a-descriptions-item label="EndTime">
          2018-04-24 18:00:00
        </a-descriptions-item>
        <a-descriptions-item label="运行时长">
          2018-04-24 18:00:00
        </a-descriptions-item>
        <a-descriptions-item label="Last ApplicationId">
          2019-04-24 18:00:00
        </a-descriptions-item>
        <a-descriptions-item label="createTime">
          2019-04-24 18:00:00
        </a-descriptions-item>
        <a-descriptions-item label="Application Type">
          Apache Flink
        </a-descriptions-item>
        <a-descriptions-item label="Status" :span="3">
          <a-badge status="processing" text="Running" />
        </a-descriptions-item>
        <a-descriptions-item label="Negotiated Amount">
          $80.00
        </a-descriptions-item>
        <a-descriptions-item label="Discount">
          $20.00
        </a-descriptions-item>
        <a-descriptions-item label="Official Receipts">
          $60.00
        </a-descriptions-item>
        <a-descriptions-item label="备份信息">
          Data disk type: MongoDB
          <br />
          Database version: 3.4
          <br />
          Package: dds.mongo.mid
          <br />
          Storage space: 10 GB
          <br />
          Replication factor: 3
          <br />
          Region: East China 1<br />
        </a-descriptions-item>
      </a-descriptions>
    </div>
  </a-card>
</template>
<script>
export default {
  data () {
    return {
      size: 'default'
    }
  },
  methods: {
    onChange (e) {
      console.log('size checked', e.target.value)
      this.size = e.target.value
    }
  }
}
</script>
