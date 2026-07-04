<script setup lang="ts">
import Quill from 'quill'
import { useTemplateRef } from 'vue'
import 'quill/dist/quill.snow.css'

defineOptions({
  name: 'RichTextEditor',
})

const { disabled } = defineProps<Props>()
interface Props {
  disabled?: boolean
}
const model = defineModel<string>()

let editorInst = null

const editorModel = ref<string>()

onMounted(() => {
  initEditor()
})

const editorRef = useTemplateRef<HTMLElement>('editorRef')
function initEditor() {
  const options = {
    modules: {
      toolbar: [
        { header: [1, 2, 3, 4, 5, 6, false] }, // 标题
        'bold', // 加粗
        'italic', // 斜体
        'strike', // 删除线
        { size: ['small', false, 'large', 'huge'] }, // 字体大小
        { font: [] }, // 字体种类
        { color: [] }, // 字体颜色、
        { background: [] }, // 字体背景颜色
        'link', // 插入链接
        'image', // 插入图片
        'blockquote', // 引用
        'link', // 超链接
        'image', // 插入图片
        'video', // 插入视频
        { list: 'bullet' }, // 无序列表
        { list: 'ordered' }, // 有序列表
        { script: 'sub' }, // 下标
        { script: 'super' }, // 上标
        { align: [] }, // 对齐方式
        'formula', // 公式
        'clean', // remove formatting button
      ],
    },

    placeholder: 'Insert text here ...',
    theme: 'snow',
  }
  const quill = new Quill(editorRef.value!, options)

  quill.on('text-change', (_delta, _oldDelta, _source) => {
    editorModel.value = quill.getSemanticHTML?.() ?? quill.root.innerHTML
  })

  if (disabled)
    quill.enable(false)

  editorInst = quill

  if (model.value)
    setContents(model.value)
}

function setContents(html: string) {
  editorInst!.setContents(editorInst!.clipboard.convert({ html }))
}

watch(
  () => model.value,
  (newValue, _oldValue) => {
    if (newValue && newValue !== editorModel.value) {
      setContents(newValue)
    }
    else if (!newValue) {
      setContents('')
    }
  },
)

watch(editorModel, (newValue, oldValue) => {
  if (newValue && newValue !== oldValue)
    model.value = newValue

  else if (!newValue)
    editorInst!.setContents([])
})

watch(
  () => disabled,
  (newValue, _oldValue) => {
    editorInst!.enable(!newValue)
  },
)

onBeforeUnmount(() => editorInst = null)
</script>

<template>
  <div ref="editorRef" />
</template>
