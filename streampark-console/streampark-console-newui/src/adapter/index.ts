import type { App } from 'vue';
import VueSweetalert2 from 'vue-sweetalert2';
import Swal from 'sweetalert2/dist/sweetalert2.js';
import 'sweetalert2/dist/sweetalert2.min.css';
import './index.less';

let swalInstance: typeof Swal | null = null;

function initSwal(options: {}) {
  if (!swalInstance) {
    swalInstance = Swal.mixin(options);
  }
}

export function swal(...args) {
  return swalInstance?.fire(...args);
}

export function setupAdapter(app: App) {
  const options = {};
  initSwal(options);
  app.use(VueSweetalert2, options);
}
