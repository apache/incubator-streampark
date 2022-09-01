{
  "config": {
    "wide_screen_mode": true
  },
  "elements": [
    {
      "tag": "note",
      "elements": [
        {
          "tag": "plain_text",
          "content": "StreamX , Make stream processing easier!"
        }
      ]
    },
    {
      "tag": "markdown",
      "content": "**Dear StreamX user:**"
    },
    {
      "tag": "note",
      "elements": [
        {
          "tag": "plain_text",
          "content": "I'm sorry to inform you that something wrong with your task"
        }
      ]
    },
    {
      "fields": [
        {
          "is_short": false,
          "text": {
            "content": "**Job Name：${jobName}**",
            "tag": "lark_md"
          }
        },
<#if  type == 1 >
        {
          "is_short": false,
          "text": {
            "content": "**Job Status：${status}**",
            "tag": "lark_md"
          }
        },
        {
          "is_short": true,
          "text": {
            "content": "**Start Time：${startTime}**",
            "tag": "lark_md"
          }
        },
        {
          "is_short": false,
          "text": {
            "content": "**End Time：${endTime}**",
            "tag": "lark_md"
          }
        },
        {
          "is_short": true,
          "text": {
            "content": "**Duration：${duration}**",
            "tag": "lark_md"
          }
        }
<#if  restart >
        ,{
          "is_short": false,
          "text": {
            "content": "**Restart：${restartIndex}/${totalRestart}**",
            "tag": "lark_md"
          }
        }
</#if>
</#if>
<#if  type == 2 >
        {
          "is_short": false,
          "text": {
            "content": "**CheckPoint Status：FAILED**",
            "tag": "lark_md"
          }
        },
        {
          "is_short": false,
          "text": {
            "content": "**Checkpoint Failure Rate Interval：${cpFailureRateInterval}**",
            "tag": "lark_md"
          }
        },
        {
          "is_short": false,
          "text": {
            "content": "**Max Failures Per Interval：${cpMaxFailureInterval}**",
            "tag": "lark_md"
          }
        },
        {
          "is_short": false,
          "text": {
            "content": "**Start Time：${startTime}**",
            "tag": "lark_md"
          }
        },
        {
          "is_short": false,
          "text": {
            "content": "**Duration：${duration}**",
            "tag": "lark_md"
          }
        }
</#if>
      ],
      "tag": "div"
    },
<#if  atAll >
    {
      "tag": "markdown",
      "content": "<at id=all></at>"
    },
</#if>
    {
      "tag": "note",
      "elements": [
        {
          "tag": "plain_text",
          "content": "Best Wishes!\nStreamX Team"
        }
      ]
    },
    {
      "tag": "action",
      "actions": [
<#if link??>{
          "tag": "button",
          "text": {
            "tag": "plain_text",
            "content": "Details"
          },
          "type": "primary",
          "url": "${link}"
        },</#if>
        {
          "tag": "button",
          "text": {
            "tag": "plain_text",
            "content": "Website"
          },
          "type": "primary",
          "url": "http://www.streamxhub.com"
        },
        {
          "tag": "button",
          "text": {
            "tag": "plain_text",
            "content": "GitHub"
          },
          "type": "primary",
          "url": "https://github.com/streamxhub/streamx"
        }
      ]
    }
  ],
  "header": {
    "template": "red",
    "title": {
      "content": "${subject}",
      "tag": "plain_text"
    }
  }
}