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
        { header: [1, 2, 3, 4, 5, 6, false] },
        'bold',
        'italic',
        'strike',
        { size: ['small', false, 'large', 'huge'] },
        { font: [] },
        { color: [] },
        { background: [] },
        'link',
        'image',
        'blockquote',
        'link',
        'image',
        'video',
        { list: 'bullet' },
        { list: 'ordered' },
        { script: 'sub' },
        { script: 'super' },
        { align: [] },
        'formula',
        'clean',
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
