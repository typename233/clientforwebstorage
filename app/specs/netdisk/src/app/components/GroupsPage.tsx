import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  ListItemButton,
  Avatar,
  Chip,
  Box,
  Card,
  CardContent,
  Fab,
} from '@mui/material';
import {
  Search,
  MoreVert,
  Add,
  Group as GroupIcon,
  FolderShared,
  Lock,
  Public,
} from '@mui/icons-material';

interface GroupItem {
  id: string;
  name: string;
  type: 'personal' | 'team';
  role: 'owner' | 'editor' | 'viewer';
  memberCount: number;
  storageUsed: string;
  visibility: 'private' | 'public';
}

const mockGroups: GroupItem[] = [
  {
    id: '1',
    name: '设计团队',
    type: 'team',
    role: 'owner',
    memberCount: 8,
    storageUsed: '23.5 GB',
    visibility: 'private',
  },
  {
    id: '2',
    name: '产品部门',
    type: 'team',
    role: 'editor',
    memberCount: 15,
    storageUsed: '67.2 GB',
    visibility: 'private',
  },
  {
    id: '3',
    name: '公开资料库',
    type: 'team',
    role: 'viewer',
    memberCount: 45,
    storageUsed: '120.8 GB',
    visibility: 'public',
  },
];

const getRoleColor = (role: string) => {
  switch (role) {
    case 'owner':
      return 'error';
    case 'editor':
      return 'primary';
    case 'viewer':
      return 'default';
    default:
      return 'default';
  }
};

const getRoleLabel = (role: string) => {
  switch (role) {
    case 'owner':
      return '所有者';
    case 'editor':
      return '编辑者';
    case 'viewer':
      return '查看者';
    default:
      return role;
  }
};

export default function GroupsPage() {
  return (
    <Box sx={{ pb: 8, height: '100vh', display: 'flex', flexDirection: 'column', bgcolor: '#fafafa' }}>
      <AppBar position="static" elevation={0}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            群组空间
          </Typography>
          <IconButton color="inherit">
            <Search />
          </IconButton>
          <IconButton color="inherit">
            <MoreVert />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Box sx={{ p: 2 }}>
        <Card elevation={0} sx={{ bgcolor: 'primary.main', color: 'white', mb: 2 }}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <FolderShared sx={{ mr: 1 }} />
              <Typography variant="h6">我的个人空间</Typography>
            </Box>
            <Typography variant="body2" sx={{ opacity: 0.9 }}>
              15.2 GB 已使用 · 100 GB 总容量
            </Typography>
          </CardContent>
        </Card>

        <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
          我的群组 ({mockGroups.length})
        </Typography>
      </Box>

      <List sx={{ flex: 1, overflow: 'auto', bgcolor: 'white' }}>
        {mockGroups.map((group) => (
          <ListItem
            key={group.id}
            disablePadding
            secondaryAction={
              <IconButton edge="end">
                <MoreVert />
              </IconButton>
            }
          >
            <ListItemButton>
              <ListItemAvatar>
                <Avatar sx={{ bgcolor: 'primary.main' }}>
                  <GroupIcon />
                </Avatar>
              </ListItemAvatar>
              <ListItemText
                primary={
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Typography variant="body1">{group.name}</Typography>
                    {group.visibility === 'private' ? (
                      <Lock fontSize="small" sx={{ color: 'text.secondary', fontSize: 16 }} />
                    ) : (
                      <Public fontSize="small" sx={{ color: 'text.secondary', fontSize: 16 }} />
                    )}
                  </Box>
                }
                secondary={
                  <Box sx={{ mt: 0.5 }}>
                    <Chip
                      label={getRoleLabel(group.role)}
                      size="small"
                      color={getRoleColor(group.role) as any}
                      sx={{ mr: 1, height: 20, fontSize: '0.7rem' }}
                    />
                    <Typography variant="caption" color="text.secondary">
                      {group.memberCount} 成员 · {group.storageUsed}
                    </Typography>
                  </Box>
                }
              />
            </ListItemButton>
          </ListItem>
        ))}
      </List>

      <Fab color="primary" sx={{ position: 'fixed', bottom: 72, right: 16 }}>
        <Add />
      </Fab>
    </Box>
  );
}
