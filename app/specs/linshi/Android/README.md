# NetDisk Android Native (Kotlin + XML)

该目录是将 `前端/netdisk` React 页面转换后的 Android 原生实现示例，采用 Kotlin + XML。

## 页面映射

- `FilesPage.tsx` -> `FilesFragment.kt` + `fragment_files.xml`
- `GroupsPage.tsx` -> `GroupsFragment.kt` + `fragment_groups.xml`
- `AgentPage.tsx` -> `AgentFragment.kt` + `fragment_agent.xml`
- `ProfilePage.tsx` -> `ProfileFragment.kt` + `fragment_profile.xml`
- `BottomNav.tsx` -> `BottomNavigationView` (`activity_main.xml` + `bottom_nav_menu.xml`)

## 目录说明

- `app/src/main/java/com/example/netdisk/`：Kotlin 源码
- `app/src/main/res/layout/`：页面布局 XML
- `app/src/main/res/menu/`：底部导航和 Toolbar 菜单
- `app/src/main/res/values/`：字符串资源

## 使用说明

将这些文件复制到 Android Studio 工程中，并确保已引入 Material 组件和 RecyclerView 依赖即可运行。
