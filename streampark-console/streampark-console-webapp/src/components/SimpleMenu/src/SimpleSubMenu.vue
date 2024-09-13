<template>
  <MenuItem
    :name="item.path"
    v-if="!menuHasChildren(item) && getShowMenu"
    v-bind="$props"
    :class="[getLevelClass, theme]"
  >
    <div v-if="collapsedShowTitle && getIsCollapseParent" class="mt-1 collapse-title">
      {{ getI18nName }}
    </div>
    <template #title>
      <span :class="[`${prefixCls}-sub-title`, getMenuItemName]">
        {{ getI18nName }}
      </span>
      <SimpleMenuTag :item="item" :collapseParent="getIsCollapseParent" />
    </template>
  </MenuItem>
  <SubMenu
    :name="item.path"
    v-if="menuHasChildren(item) && getShowMenu"
    :class="[getLevelClass, theme]"
    :collapsedShowTitle="collapsedShowTitle"
  >
    <template #title>
      <span class="menu-down-svg">
        <SvgIcon v-if="item.path === '/system'" name="management" size="20" />
        <SvgIcon v-if="item.path === '/flink'" name="flink3" size="20" />
        <SvgIcon v-if="item.path === '/spark'" name="spark" size="20" />
        <SvgIcon v-if="item.path === '/setting'" name="settings" size="20" />
        <SvgIcon v-if="item.path === '/resource'" name="resource" size="20" />
      </span>
      <div v-if="collapsedShowTitle && getIsCollapseParent" class="mt-2 collapse-title">
        {{ getI18nName }}
      </div>
      <span v-show="getShowSubTitle" :class="['ml-2', `${prefixCls}-sub-title`, getMenuItemName]">
        {{ getI18nName }}
      </span>
      <SimpleMenuTag :item="item" :collapseParent="!!collapse && !!parent" />
    </template>
    <template v-for="childrenItem in item.children || []" :key="childrenItem.path">
      <SimpleSubMenu v-bind="$props" :item="childrenItem" :parent="false" />
    </template>
  </SubMenu>
</template>
<script lang="ts">
  import type { PropType } from 'vue';
  import type { Menu } from '/@/router/types';

  import { defineComponent, computed } from 'vue';
  import { useDesign } from '/@/hooks/web/useDesign';

  import MenuItem from './components/MenuItem.vue';
  import SubMenu from './components/SubMenuItem.vue';
  import { propTypes } from '/@/utils/propTypes';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { createAsyncComponent } from '/@/utils/factory/createAsyncComponent';
  import SvgIcon from '/@/components/Icon/src/SvgIcon.vue';

  export default defineComponent({
    name: 'SimpleSubMenu',
    components: {
      SvgIcon,
      SubMenu,
      MenuItem,
      SimpleMenuTag: createAsyncComponent(() => import('./SimpleMenuTag.vue')),
    },
    props: {
      item: {
        type: Object as PropType<Menu>,
        default: () => ({}),
      },
      parent: propTypes.bool,
      collapsedShowTitle: propTypes.bool,
      collapse: propTypes.bool,
      theme: propTypes.oneOf(['dark', 'light']),
    },
    setup(props) {
      const { t } = useI18n();
      const { prefixCls } = useDesign('simple-menu');
      const getShowMenu = computed(() => !props.item?.meta?.hidden);
      const getIcon = computed(() =>
        props.item?.icon ? `ant-design:${props.item?.icon}-outlined` : '',
      );
      const getI18nName = computed(() => t(props.item?.name));
      const getMenuItemName = computed(() => {
        return 'menu-item-' + props.item?.path.substring(1).replaceAll('/', '_');
      });
      const getShowSubTitle = computed(() => !props.collapse || !props.parent);
      const getIsCollapseParent = computed(() => !!props.collapse && !!props.parent);
      const getLevelClass = computed(() => {
        return [
          {
            [`${prefixCls}__parent`]: props.parent,
            [`${prefixCls}__children`]: !props.parent,
          },
        ];
      });

      function menuHasChildren(menuTreeItem: Menu): boolean {
        return (
          !menuTreeItem.meta?.hideChildrenInMenu &&
          Reflect.has(menuTreeItem, 'children') &&
          !!menuTreeItem.children &&
          menuTreeItem.children.length > 0
        );
      }

      return {
        prefixCls,
        menuHasChildren,
        getShowMenu,
        getIcon,
        getI18nName,
        getShowSubTitle,
        getLevelClass,
        getMenuItemName,
        getIsCollapseParent,
      };
    },
  });
</script>
