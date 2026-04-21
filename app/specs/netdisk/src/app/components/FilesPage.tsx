import { useState } from 'react';
import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemButton,
  Fab,
  Menu,
  MenuItem,
  Chip,
  Box,
  LinearProgress,
} from '@mui/material';
import {
  Search,
  MoreVert,
  InsertDriveFile,
  Folder,
  Add,
  CreateNewFolder,
  Upload,
  Image,
  VideoFile,
  Description,
} from '@mui/icons-material';

interface FileItem {
  id: string;
  name: string;
  type: 'file' | 'folder';
  size?: string;
  extension?: string;
  updatedAt: string;
}

const mockFiles: FileItem[] = [
  { id: '1', name: '工作文档', type: 'folder', updatedAt: '2026-04-20' },
  { id: '2', name: '个人照片', type: 'folder', updatedAt: '2026-04-19' },
  { id: '3', name: '项目报告.pdf', type: 'file', size: '2.5 MB', extension: 'pdf', updatedAt: '2026-04-21' },
  { id: '4', name: '设计稿.png', type: 'file', size: '1.8 MB', extension: 'png', updatedAt: '2026-04-20' },
  { id: '5', name: '演示视频.mp4', type: 'file', size: '45.2 MB', extension: 'mp4', updatedAt: '2026-04-18' },
  { id: '6', name: '会议记录.docx', type: 'file', size: '156 KB', extension: 'docx', updatedAt: '2026-04-17' },
];

export default function FilesPage() {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [fabMenuAnchor, setFabMenuAnchor] = useState<null | HTMLElement>(null);

  const getFileIcon = (item: FileItem) => {
    if (item.type === 'folder') return <Folder sx={{ color: '#FFB74D' }} />;

    switch (item.extension) {
      case 'png':
      case 'jpg':
      case 'jpeg':
        return <Image sx={{ color: '#4CAF50' }} />;
      case 'mp4':
      case 'avi':
        return <VideoFile sx={{ color: '#F44336' }} />;
      case 'pdf':
      case 'docx':
      case 'doc':
        return <Description sx={{ color: '#2196F3' }} />;
      default:
        return <InsertDriveFile sx={{ color: '#9E9E9E' }} />;
    }
  };

  return (
    <Box sx={{ pb: 8, height: '100vh', display: 'flex', flexDirection: 'column', bgcolor: '#fafafa' }}>
      <AppBar position="static" elevation={0}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            我的文件
          </Typography>
          <IconButton color="inherit">
            <Search />
          </IconButton>
          <IconButton color="inherit" onClick={(e) => setAnchorEl(e.currentTarget)}>
            <MoreVert />
          </IconButton>
        </Toolbar>
        <Box sx={{ px: 2, pb: 2 }}>
          <Box sx={{ display: 'flex', gap: 1, overflowX: 'auto', pb: 1 }}>
            <Chip label="全部" color="primary" size="small" />
            <Chip label="图片" variant="outlined" size="small" />
            <Chip label="文档" variant="outlined" size="small" />
            <Chip label="视频" variant="outlined" size="small" />
          </Box>
        </Box>
      </AppBar>

      <Box sx={{ px: 2, pt: 2, bgcolor: 'white' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="caption" color="text.secondary">
            存储空间
          </Typography>
          <Typography variant="caption" color="text.secondary">
            15.2 GB / 100 GB
          </Typography>
        </Box>
        <LinearProgress variant="determinate" value={15.2} sx={{ height: 6, borderRadius: 3 }} />
      </Box>

      <List sx={{ flex: 1, overflow: 'auto', bgcolor: 'white', mt: 1 }}>
        {mockFiles.map((file) => (
          <ListItem
            key={file.id}
            disablePadding
            secondaryAction={
              <IconButton edge="end">
                <MoreVert />
              </IconButton>
            }
          >
            <ListItemButton>
              <ListItemIcon>{getFileIcon(file)}</ListItemIcon>
              <ListItemText
                primary={file.name}
                secondary={
                  <span>
                    {file.size && `${file.size} · `}
                    {file.updatedAt}
                  </span>
                }
              />
            </ListItemButton>
          </ListItem>
        ))}
      </List>

      <Fab
        color="primary"
        sx={{ position: 'fixed', bottom: 72, right: 16 }}
        onClick={(e) => setFabMenuAnchor(e.currentTarget)}
      >
        <Add />
      </Fab>

      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
        <MenuItem onClick={() => setAnchorEl(null)}>排序方式</MenuItem>
        <MenuItem onClick={() => setAnchorEl(null)}>查看方式</MenuItem>
        <MenuItem onClick={() => setAnchorEl(null)}>刷新</MenuItem>
      </Menu>

      <Menu anchorEl={fabMenuAnchor} open={Boolean(fabMenuAnchor)} onClose={() => setFabMenuAnchor(null)}>
        <MenuItem onClick={() => setFabMenuAnchor(null)}>
          <ListItemIcon>
            <CreateNewFolder fontSize="small" />
          </ListItemIcon>
          <ListItemText>新建文件夹</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => setFabMenuAnchor(null)}>
          <ListItemIcon>
            <Upload fontSize="small" />
          </ListItemIcon>
          <ListItemText>上传文件</ListItemText>
        </MenuItem>
      </Menu>
    </Box>
  );
}
