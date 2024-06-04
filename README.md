# hexo-auto-submit-urls
使用 GitHub Actions 每天自动推送网站到 Bing 和 Bing 的 IndexNow 以及谷歌、百度

1. 本项目是利用 `hexo-generator-feed` 生成的 RSS 进行解析并获取文章列表，所以需要你在 Hexo 安装该插件

   ```shell
   npm install hexo-generator-feed --save
   ```

   然后在博客的配置文件里添加如下配置

   ```yaml
   feed:
     enable: true
     type: atom
     path: atom.xml
     limit: 0
     hub:
     content: true
     content_limit:
     content_limit_delim: ' '
     order_by: -date
     icon: 图标.icon
     autodiscovery: true
     template:
   ```

   详情见插件地址：[hexojs/hexo-generator-feed](https://github.com/hexojs/hexo-generator-feed)

2. 【[Fork](https://github.com/sudojia/hexo-auto-submit-urls/fork)】本项目并在仓库的 `settings -> Secrets and variables -> Actions -> New repository secret` 添加环境变量

   - 详情见【[参数说明](https://blog.imzjw.cn/posts/3ed40d11/#%E5%8F%82%E6%95%B0%E8%AF%B4%E6%98%8E)】
