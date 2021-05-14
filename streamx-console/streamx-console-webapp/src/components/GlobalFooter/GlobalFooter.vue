<template>
  <div class="footer">
    <div class="links">
      <a
        href="https://github.com/streamxhub/streamx"
        target="_blank">
        <a-icon type="github" />
      </a>
    </div>
    <div class="copyright">
      Copyright<a-icon type="copyright" /> 2019~{{ year }} <span>StreamX</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'GlobalFooter',
  data () {
    return {
      year: new Date().getFullYear()
    }
  }
}
</script>

<style lang="less" scoped>
.footer {
  padding: 0 16px;
  margin: 48px 0 24px;
  text-align: center;

  .links {
    margin-bottom: 8px;

    a {
      color: rgba(0, 0, 0, 0.45);

      &:hover {
        color: rgba(0, 0, 0, 0.65);
      }

      &:not(:last-child) {
        margin-right: 40px;
      }
    }
  }
  .copyright {
    color: rgba(0, 0, 0, 0.45);
    font-size: 14px;
  }
}
</style>
