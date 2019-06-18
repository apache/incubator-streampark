<template>
  <a-skeleton active :loading="loading" :paragraph="{rows: 24}">
  <div style="padding: 10px;">
    <a-row>
      <a-col :span="span" v-for="(movie, index) in movies" :key="index" style="padding: 1rem;">
        <a-card class="movie-card" style="width: 200px" @click="detail(movie.id)">
          <img :src="movie.image" style="width: 198px; height: 300px;" alt="海报"/>
          <div class="movie-desc">
            <div class="movie-title" :title="movie.title"><p>{{movie.title}}</p></div>
            <p style="color: #aaa;margin-bottom: .2rem">{{movie.type}}</p>
            <p>{{movie.releaseDate}}</p>
          </div>
        </a-card>
      </a-col>
    </a-row>
    <!-- 电影详情 -->
    <a-modal
      class="movie-detail"
      v-model="detailShow"
      :width="700"
      :keyboard="false"
      :centered="true"
      @cancel="detailClose"
      :maskClosable="false"
      :mask="false"
      :footer="null">
      <a-tabs ref="movieTabs" :activeKey="activeKey" @change="changeTab">
        <a-tab-pane tab="电影详情" key="1">
          <a-skeleton active :loading="detailLoading" :paragraph="{rows: 14}">
            <span>
              <a-row :gutter="2">
                <a-col :span="12" class="poster">
                  <img alt="电影海报" :src="movieDetail.basic.img" class="poster-img">
                </a-col>
                <a-col :span="12">
                  <p>片名：{{movieDetail.basic.name}}</p>
                  <p>片长：{{movieDetail.basic.mins}}</p>
                  <p>是否3D：{{movieDetail.basic.is3D ? '是' : '否'}}</p>
                  <p>是否IMAX：{{movieDetail.basic.isIMAX ? '是' : '否'}}</p>
                  <p>上映日期：{{movieDetail.basic.releaseDate}}</p>
                  <p>上映国家 / 地区：{{movieDetail.basic.releaseArea}}</p>
                  <p>综合评分：{{movieDetail.basic.overallRating === -1 ? '暂无评分' : movieDetail.basic.overallRating}}</p>
                  <p v-if="movieDetail.boxOffice.todayBoxDesUnit">{{movieDetail.boxOffice.todayBoxDesUnit}}：{{movieDetail.boxOffice.todayBoxDes}}</p>
                  <p v-if="movieDetail.boxOffice.totalBoxUnit">{{movieDetail.boxOffice.totalBoxUnit}}：{{movieDetail.boxOffice.totalBoxDes}}</p>
                  <p>预告片：<a @click="video(movieDetail.basic.video.hightUrl)">点击查看</a></p>
                  <a-popover :title="movieDetail.basic.director.name">
                    <template slot="content">
                      <img alt="头像" :src="movieDetail.basic.director.img" style="width:140px; height: 180px"/>
                    </template>
                    <p>导演：<a>{{movieDetail.basic.director.name}}</a></p>
                  </a-popover>
                  <div style="margin-bottom: .4rem">
                    主演：
                    <template v-for="(actor, index) in movieDetail.basic.actors">
                      <a-popover :key="index" :title="actor.name">
                        <template slot="content">
                          <div>
                            <p style="max-width: 140px">扮演：{{actor.roleName ? actor.roleName : '未知 X_X'}}</p>
                            <img alt="头像" :src="actor.img" style="width:140px; height: 180px"/>
                          </div>
                        </template>
                        <span v-if="actor.name">
                          <span v-if="index === 0"><a style="margin-left: -6px;">{{actor.name}}</a> · </span>
                          <span v-else-if="index === movieDetail.basic.actors.length - 1"><a>{{actor.name}}</a></span>
                          <span v-else><a>{{actor.name}}</a> · </span>
                        </span>
                      </a-popover>
                    </template>
                  </div>
                </a-col>
              </a-row>
              <a-row>
                <a-col>
                  <a-divider orientation="left">电影简介</a-divider>
                  <div style="text-indent: 1rem;margin-bottom: 2rem">{{movieDetail.basic.story}}</div>
                </a-col>
              </a-row>
            </span>
          </a-skeleton>
        </a-tab-pane>
        <a-tab-pane tab="电影评论" key="2">
          <div>
            <a-list
              itemLayout="horizontal"
              :dataSource="comments">
              <a-list-item slot="renderItem" slot-scope="comment, index">
                <a-list-item-meta
                  :description="comment.content">
                  <div slot="title">
                    {{comment.nickname}}&nbsp;
                    <span style="color: #aaa;font-weight: 400; font-size: 13px">{{getDate(comment.commentDate)}}</span>
                  </div>
                  <a-avatar slot="avatar" :src="comment.headImg" />
                </a-list-item-meta>
              </a-list-item>
            </a-list>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
  </a-skeleton>
</template>
<script>
export default {
  name: 'MovieComing',
  data () {
    return {
      loading: true,
      detailShow: false,
      movies: [],
      span: '',
      screenWidth: document.body.clientWidth,
      timer: null,
      movieDetail: {
        boxOffice: {
          todayBoxDesUnit: '',
          todayBoxDes: '',
          totalBoxUnit: '',
          totalBoxDes: ''
        },
        basic: {
          img: '',
          actors: [],
          director: {
            img: '',
            name: ''
          }
        }
      },
      activeKey: '1',
      detailLoading: true,
      comments: []
    }
  },
  methods: {
    detail (id) {
      this.activeKey = '1'
      this.detailShow = true
      this.detailLoading = true
      this.$get('movie/detail?id=' + id).then((r) => {
        let data = JSON.parse(r.data.data)
        data = data.data
        this.movieDetail = data
        this.detailLoading = false
        this.$get('movie/comments?id=' + id).then((r) => {
          let data = JSON.parse(r.data.data)
          data = data.data
          this.comments = this.comments.concat(data.mini.list)
          this.comments = this.comments.concat(data.plus.list)
        })
      })
    },
    detailClose () {
      this.detailShow = false
      this.comments = []
    },
    video (url) {
      window.open(url)
    },
    changeTab (tab) {
      this.activeKey = tab
    },
    getDate (tm) {
      return new Date(tm * 1000).toLocaleString()
    }
  },
  mounted () {
    const that = this
    let val = window.innerWidth
    if (val > 1660) {
      this.span = 4
    } else if (val > 1250) {
      this.span = 6
    } else if (val > 1000) {
      this.span = 8
    } else if (val > 900) {
      this.span = 10
    } else {
      this.span = 12
    }
    window.onresize = () => {
      return (() => {
        window.screenWidth = document.body.clientWidth
        that.screenWidth = window.screenWidth
      })()
    }
    this.$get('movie/coming').then((r) => {
      let data = JSON.parse(r.data.data)
      this.movies = data.moviecomings
      this.loading = false
    })
  },
  watch: {
    screenWidth (val) {
      if (this.timer) {
        clearTimeout(this.timer)
      }
      this.timer = setTimeout(() => {
        this.screenWidth = val
        if (val > 1660) {
          this.span = 4
        } else if (val > 1250) {
          this.span = 6
        } else if (val > 1000) {
          this.span = 8
        } else if (val > 900) {
          this.span = 10
        } else {
          this.span = 12
        }
      }, 100)
    }
  }
}
</script>
<style lang="less">
  .movie-card {
    .ant-card-body {
      padding: 0 !important;
    }
    .movie-desc {
      padding: 1rem 0 0 0;
      text-align: center;
      .movie-title {
        font-size: .9rem;
        font-weight: 600;
        margin-bottom: .2rem;
        p {
          margin-bottom: 0 !important;
        }
      }
    }
    &:hover {
      cursor: pointer;
      box-shadow: 4px 0 8px 0 rgba(29, 35, 41, 0.1);
    }
  }
  .movie-detail {
    p {
      margin-bottom: .4rem;
    }
    .poster {
      margin-right: -4rem;
      .poster-img {
        width: 198px;
        height: 300px;
      }
    }
  }
</style>
