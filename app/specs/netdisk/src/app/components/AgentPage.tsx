import { useState } from 'react';
import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Box,
  TextField,
  InputAdornment,
  Card,
  CardContent,
  Avatar,
  Chip,
  List,
  ListItem,
  ListItemText,
  Divider,
} from '@mui/material';
import {
  MoreVert,
  Send,
  SmartToy,
  AutoAwesome,
  Search as SearchIcon,
  Upload,
  Share,
  Summarize,
} from '@mui/icons-material';

interface Message {
  id: string;
  type: 'user' | 'agent';
  content: string;
  timestamp: string;
}

const mockMessages: Message[] = [
  {
    id: '1',
    type: 'agent',
    content: '您好!我是您的智能助手,可以帮您搜索文件、整理资料、生成摘要等。有什么我可以帮您的吗?',
    timestamp: '10:30',
  },
];

const suggestions = [
  { icon: <SearchIcon fontSize="small" />, label: '搜索最近修改的文档' },
  { icon: <Upload fontSize="small" />, label: '整理上个月的照片' },
  { icon: <Share fontSize="small" />, label: '创建分享链接' },
  { icon: <Summarize fontSize="small" />, label: '生成文件夹摘要' },
];

export default function AgentPage() {
  const [messages] = useState<Message[]>(mockMessages);
  const [input, setInput] = useState('');

  return (
    <Box sx={{ pb: 8, height: '100vh', display: 'flex', flexDirection: 'column', bgcolor: '#fafafa' }}>
      <AppBar position="static" elevation={0}>
        <Toolbar>
          <Avatar sx={{ bgcolor: 'secondary.main', width: 40, height: 40, mr: 2 }}>
            <SmartToy />
          </Avatar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            智能助手
          </Typography>
          <IconButton color="inherit">
            <MoreVert />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'center', mb: 3, mt: 2 }}>
          <Box sx={{ textAlign: 'center' }}>
            <Avatar sx={{ bgcolor: 'secondary.main', width: 64, height: 64, margin: '0 auto', mb: 1 }}>
              <AutoAwesome sx={{ fontSize: 32 }} />
            </Avatar>
            <Typography variant="h6" gutterBottom>
              AI 智能助手
            </Typography>
            <Typography variant="body2" color="text.secondary">
              让我帮您更高效地管理文件
            </Typography>
          </Box>
        </Box>

        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1.5 }}>
            快速操作
          </Typography>
          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1 }}>
            {suggestions.map((suggestion, index) => (
              <Card key={index} elevation={0} sx={{ border: '1px solid #e0e0e0' }}>
                <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box sx={{ color: 'primary.main' }}>{suggestion.icon}</Box>
                    <Typography variant="caption" sx={{ lineHeight: 1.3 }}>
                      {suggestion.label}
                    </Typography>
                  </Box>
                </CardContent>
              </Card>
            ))}
          </Box>
        </Box>

        <Divider sx={{ my: 2 }}>
          <Chip label="对话记录" size="small" />
        </Divider>

        <List>
          {messages.map((message) => (
            <ListItem
              key={message.id}
              sx={{
                display: 'flex',
                justifyContent: message.type === 'user' ? 'flex-end' : 'flex-start',
                px: 0,
              }}
            >
              <Card
                elevation={0}
                sx={{
                  maxWidth: '80%',
                  bgcolor: message.type === 'user' ? 'primary.main' : 'white',
                  color: message.type === 'user' ? 'white' : 'text.primary',
                  border: message.type === 'agent' ? '1px solid #e0e0e0' : 'none',
                }}
              >
                <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
                  <ListItemText
                    primary={message.content}
                    secondary={message.timestamp}
                    secondaryTypographyProps={{
                      sx: {
                        color: message.type === 'user' ? 'rgba(255,255,255,0.7)' : 'text.secondary',
                        fontSize: '0.7rem',
                        mt: 0.5,
                      },
                    }}
                  />
                </CardContent>
              </Card>
            </ListItem>
          ))}
        </List>
      </Box>

      <Box sx={{ p: 2, bgcolor: 'white', borderTop: '1px solid #e0e0e0' }}>
        <TextField
          fullWidth
          placeholder="输入消息..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          variant="outlined"
          size="small"
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <IconButton color="primary" disabled={!input.trim()}>
                  <Send />
                </IconButton>
              </InputAdornment>
            ),
          }}
        />
      </Box>
    </Box>
  );
}
