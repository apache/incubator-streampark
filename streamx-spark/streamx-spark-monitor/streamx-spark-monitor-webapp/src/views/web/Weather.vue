<template>
  <div style="width: 100%; padding: 0 1rem">
    <div class="global-search-main">
      <div class="global-search-wrapper">
        <a-auto-complete
          :dataSource="dataSource"
          class="global-search"
          @select="onSelect"
          @search="handleSearch"
          placeholder="请输入城市名">
          <a-input>
            <a-button slot="suffix" class="search-btn" type="primary" @click.stop="searchWeather">
              <a-icon type="search"/>
            </a-button>
          </a-input>
        </a-auto-complete>
      </div>
    </div>
    <div class="weather-area" v-show="!loading">
      <div class="weather-chart-info">
        <apexchart ref="seven" height="350" type=line :options="seven.chartOptions" :series="seven.series" />
      </div>
      <div class="weather-chart-info">
        <apexchart ref="future" height="350" type=area :options="future.chartOptions" :series="future.series" />
      </div>
    </div>
    <div class="weather-area">
      <div class="weather-info">
        <a-card :loading="loading" :title="this.weather.countyName + ' 当前天气'">
          <a-row>
            <a-col :span="8"><p>天气：{{this.weather.realtime.weather}}</p></a-col>
            <a-col :span="8"><p>风向：{{this.weather.realtime.wD}}</p></a-col>
            <a-col :span="8"><p>风力大小：{{this.weather.realtime.wS}}</p></a-col>
          </a-row>
          <a-row>
            <a-col :span="8"><p>温度：{{this.weather.realtime.temp}}℃</p></a-col>
            <a-col :span="8"><p>体感温度：{{this.weather.realtime.sendibleTemp}}℃</p></a-col>
            <a-col :span="8"><p>空气湿度：{{this.weather.realtime.sD}}%</p></a-col>
          </a-row>
          <a-row>
            <a-col :span="8"><p>更新时间：{{this.weather.realtime.time}}</p></a-col>
          </a-row>
        </a-card>
      </div>
      <div class="weather-info">
        <a-card :loading="loading" :title="this.weather.countyName + ' 未来天气'">
          <a-row>
            <a-col :span="24" v-for="(w, index) in this.weather.weathers" :key="index">
              <template v-if="index !== 6">
                <p>{{w.date}}【{{w.week}}】：日出时间 --- {{w.sun_rise_time}}      日落时间 --- {{w.sun_down_time}}      天气 --- {{w.weather}}</p>
              </template>
            </a-col>
          </a-row>
        </a-card>
      </div>
      <div class="weather-info">
        <a-card :loading="loading" :title="this.weather.countyName + ' 生活指数'">
          <a-row>
            <a-col :span="24" v-for="(i, index) in this.weather.indexes" :key="index">
              <p>{{i.name}} --- {{i.content}}</p>
            </a-col>
          </a-row>
        </a-card>
      </div>
      <div class="weather-info" v-if="this.weather.alarms.length">
        <a-card :loading="loading" :title="this.weather.countyName + ' 预警信息'">
          <a-col :span="24">
            <p>预警标题 --- {{this.weather.alarms[0].alarmDesc}}</p>
            <p>预警类型 --- {{this.weather.alarms[0].alarmTypeDesc}}</p>
            <p>预警等级 --- {{this.weather.alarms[0].alarmLevelNoDesc}}</p>
            <p>发布时间 --- {{this.weather.alarms[0].publishTime}}</p>
            <p>预防措施 --- {{this.weather.alarms[0].precaution}}</p>
            <a-popover title="预警详情">
              <template slot="content">
                <div style="max-width: 360px">{{this.weather.alarms[0].alarmContent}}</div>
              </template>
              <p>预警详情 --- {{this.weather.alarms[0].alarmContent}}</p>
            </a-popover>
          </a-col>
        </a-card>
      </div>
    </div>
  </div>
</template>
<script>
import axios from 'axios'

export default {
  data () {
    return {
      loading: true,
      seven: {
        series: [],
        chartOptions: {
          chart: {
            shadow: {
              enabled: true,
              color: '#000',
              top: 18,
              left: 7,
              blur: 10,
              opacity: 1
            },
            toolbar: {
              show: false
            }
          },
          colors: ['#f5564e', '#35d0ba'],
          dataLabels: {
            enabled: true
          },
          stroke: {
            curve: 'smooth'
          },
          markers: {
            size: 4
          },
          xaxis: {},
          yaxis: {}
        }
      },
      future: {
        series: [],
        chartOptions: {
          chart: {
            toolbar: {
              show: false
            }
          },
          dataLabels: {
            enabled: false
          },
          stroke: {
            curve: 'smooth'
          },
          xaxis: {}
        }
      },
      dataSource: [],
      storage: [],
      citys: [],
      areaId: '',
      weather: {
        provinceName: '',
        countyName: '',
        weathers: [],
        day_c: [],
        night_c: [],
        hours_c: [],
        dateArr: [],
        timeArr: [],
        publishTime: '',
        alarms: [],
        realtime: {},
        indexes: []
      }
    }
  },
  mounted () {
    axios.get('../../../static/file/city.json').then((r) => {
      this.citys = r.data
    })
  },
  methods: {
    handleSearch (value) {
      this.dataSource = []
      this.storage = []
      this.areaId = ''
      if (!value) {
        return
      }
      for (let i = 0; i < this.citys.length; i++) {
        let currentCity = this.citys[i]
        if (currentCity.countyname.indexOf(value) !== -1) {
          this.dataSource.push(currentCity.countyname)
          this.storage.push(currentCity.areaid)
        }
      }
    },
    onSelect (value) {
      let index = this.dataSource.indexOf(value)
      this.areaId = this.storage[index]
    },
    searchWeather () {
      if (!this.areaId) {
        this.$message.warning('请选择城市')
      } else {
        this.$get('weather?areaId=' + this.areaId).then((r) => {
          let data = JSON.parse(r.data.data)
          if (data.code === '200') {
            this.weather = {
              countyName: '',
              weathers: [],
              day_c: [],
              night_c: [],
              hours_c: [],
              dateArr: [],
              timeArr: [],
              publishTime: ''
            }
            this.loading = false
            this.weather.provinceName = data.value[0].provinceName
            this.weather.countyName = data.value[0].city
            this.weather.weathers = data.value[0].weathers
            this.weather.alarms = data.value[0].alarms
            this.weather.realtime = data.value[0].realtime
            this.weather.indexes = data.value[0].indexes
            let weathers = this.weather.weathers
            let min = 0
            let max = 0
            for (let i = 0; i < weathers.length; i++) {
              if (i === weathers.length - 1) {
                this.weather.day_c.unshift(parseFloat(weathers[i].temp_day_c))
                this.weather.night_c.unshift(parseFloat(weathers[i].temp_night_c))
                this.weather.dateArr.unshift(weathers[i].date.split('-')[1] + '-' + weathers[i].date.split('-')[2])
              } else {
                this.weather.day_c.push(parseFloat(weathers[i].temp_day_c))
                this.weather.night_c.push(parseFloat(weathers[i].temp_night_c))
                this.weather.dateArr.push(weathers[i].date.split('-')[1] + '-' + weathers[i].date.split('-')[2])
              }
              if (i === 0) {
                max = this.weather.day_c[0]
                min = this.weather.night_c[0]
              } else {
                if (this.weather.day_c[i] > max) {
                  max = this.weather.day_c[i]
                }
                if (this.weather.night_c[i] < min) {
                  min = this.weather.night_c[i]
                }
              }
            }
            let weather3HoursDetailsInfos = data.value[0].weatherDetailsInfo.weather3HoursDetailsInfos
            this.weather.publishTime = data.value[0].weatherDetailsInfo.publishTime
            for (let i = 0; i < weather3HoursDetailsInfos.length; i++) {
              let time = weather3HoursDetailsInfos[i].endTime.split(' ')[1].split(':')
              this.weather.hours_c.push(parseFloat(weather3HoursDetailsInfos[i].highestTemperature))
              this.weather.timeArr.push(time[0] + ':' + time[1])
            }
            this.$refs.seven.updateSeries([
              {
                name: '最高温',
                data: this.weather.day_c
              },
              {
                name: '最低温',
                data: this.weather.night_c
              }
            ], true)
            this.$refs.future.updateSeries([
              {
                name: '未来气温',
                data: this.weather.hours_c
              }
            ])
            this.$refs.seven.updateOptions({
              xaxis: {
                categories: this.weather.dateArr
              },
              yaxis: {
                min: min - 5,
                max: max + 5
              },
              title: {
                text: `${this.weather.provinceName} - ${this.weather.countyName}未来七日气温`,
                align: 'center'
              }
            }, true, true)
            this.$refs.future.updateOptions({
              xaxis: {
                categories: this.weather.timeArr
              },
              title: {
                text: `${this.weather.provinceName} - ${this.weather.countyName}未来气温细节`,
                align: 'center'
              }
            }, true, true)
          }
        }).catch((r) => {
          console.error(r)
          this.$message.error('天气查询失败')
        })
      }
    }
  }
}
</script>
<style lang="less">
  .global-search-main {
    margin-bottom: 2.5rem;
    .global-search-wrapper {
      width: 300px;
      margin:0 auto;
      .global-search {
        width: 100%;
      }
    }
  }
  .weather-area {
    display: inline;
    .weather-chart-info {
      width: 49%;
      display:inline-block;
    }
    .weather-info {
      margin: .5rem 0;
      width: 100%;
      display: inline-block;
      p {
        margin-bottom: .4rem !important;
      }
    }
  }
  .global-search.ant-select-auto-complete {
    .ant-select-selection--single {
      margin-right: -46px;
    }
    .ant-input-affix-wrapper {
      .ant-input:not(:last-child) {
        padding-right: 62px;
      }
      .ant-input-suffix {
        right: 0;
      }
      .ant-input-suffix button {
        border-top-left-radius: 0;
        border-bottom-left-radius: 0;
      }
    }
  }
</style>
