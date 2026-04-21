import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Box,
  Avatar,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemButton,
  Divider,
  Card,
  CardContent,
  LinearProgress,
  Switch,
} from '@mui/material';
import {
  Settings,
  CloudQueue,
  Share,
  History,
  Security,
  Notifications,
  Language,
  Info,
  ExitToApp,
  ChevronRight,
  Star,
} from '@mui/icons-material';

interface MenuItem {
  icon: JSX.Element;
  label: string;
  action?: string;
  hasSwitch?: boolean;
  showChevron?: boolean;
}

const menuSections = [
  {
    title: '存储与分享',
    items: [
      { icon: <CloudQueue />, label: '存储管理', showChevron: true },
      { icon: <Share />, label: '我的分享', showChevron: true },
      { icon: <History />, label: '操作记录', showChevron: true },
      { icon: <Star />, label: '收藏夹', showChevron: true },
    ],
  },
  {
    title: '设置',
    items: [
      { icon: <Notifications />, label: '消息通知', hasSwitch: true },
      { icon: <Security />, label: '隐私与安全', showChevron: true },
      { icon: <Language />, label: '语言设置', showChevron: true },
    ],
  },
  {
    title: '其他',
    items: [
      { icon: <Info />, label: '关于我们', showChevron: true },
      { icon: <Settings />, label: '账号设置', showChevron: true },
    ],
  },
];

export default function ProfilePage() {
  return (
    <Box sx={{ pb: 8, height: '100vh', display: 'flex', flexDirection: 'column', bgcolor: '#fafafa' }}>
      <AppBar position="static" elevation={0}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            我的
          </Typography>
        </Toolbar>
      </AppBar>

      <Box sx={{ overflow: 'auto', flex: 1 }}>
        <Card elevation={0} sx={{ m: 2, mb: 3 }}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
              <Avatar
                sx={{ width: 64, height: 64, mr: 2, bgcolor: 'primary.main' }}
                alt="Tom"
              >
                T
              </Avatar>
              <Box>
                <Typography variant="h6">Tom</Typography>
                <Typography variant="body2" color="text.secondary">
                  user@example.com
                </Typography>
              </Box>
            </Box>

            <Divider sx={{ my: 2 }} />

            <Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                <Typography variant="body2" color="text.secondary">
                  存储空间使用情况
                </Typography>
                <Typography variant="body2" fontWeight="medium">
                  15.2 GB / 100 GB
                </Typography>
              </Box>
              <LinearProgress variant="determinate" value={15.2} sx={{ height: 6, borderRadius: 3 }} />
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                <Typography variant="caption" color="text.secondary">
                  剩余 84.8 GB
                </Typography>
                <Typography variant="caption" color="primary" fontWeight="medium">
                  升级容量
                </Typography>
              </Box>
            </Box>
          </CardContent>
        </Card>

        {menuSections.map((section, sectionIndex) => (
          <Box key={sectionIndex} sx={{ mb: 2 }}>
            <Typography variant="caption" color="text.secondary" sx={{ px: 2, display: 'block', mb: 1 }}>
              {section.title}
            </Typography>
            <List sx={{ bgcolor: 'white', py: 0 }}>
              {section.items.map((item, itemIndex) => (
                <Box key={itemIndex}>
                  <ListItem
                    disablePadding
                    secondaryAction={
                      item.hasSwitch ? (
                        <Switch edge="end" />
                      ) : item.showChevron ? (
                        <ChevronRight sx={{ color: 'text.secondary' }} />
                      ) : null
                    }
                  >
                    <ListItemButton>
                      <ListItemIcon sx={{ minWidth: 40 }}>
                        {item.icon}
                      </ListItemIcon>
                      <ListItemText primary={item.label} />
                    </ListItemButton>
                  </ListItem>
                  {itemIndex < section.items.length - 1 && <Divider variant="inset" component="li" />}
                </Box>
              ))}
            </List>
          </Box>
        ))}

        <List sx={{ bgcolor: 'white', mb: 2 }}>
          <ListItem disablePadding>
            <ListItemButton sx={{ color: 'error.main' }}>
              <ListItemIcon sx={{ minWidth: 40 }}>
                <ExitToApp sx={{ color: 'error.main' }} />
              </ListItemIcon>
              <ListItemText primary="退出登录" />
            </ListItemButton>
          </ListItem>
        </List>

        <Box sx={{ textAlign: 'center', py: 2 }}>
          <Typography variant="caption" color="text.secondary">
            NetDisk v1.0.0
          </Typography>
        </Box>
      </Box>
    </Box>
  );
}
