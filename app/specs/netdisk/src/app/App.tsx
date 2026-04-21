import { useState } from 'react';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { CssBaseline, Box } from '@mui/material';
import BottomNav from './components/BottomNav';
import FilesPage from './components/FilesPage';
import GroupsPage from './components/GroupsPage';
import AgentPage from './components/AgentPage';
import ProfilePage from './components/ProfilePage';

const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#9c27b0',
    },
  },
  typography: {
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
  },
});

export default function App() {
  const [currentTab, setCurrentTab] = useState(0);

  const renderPage = () => {
    switch (currentTab) {
      case 0:
        return <FilesPage />;
      case 1:
        return <GroupsPage />;
      case 2:
        return <AgentPage />;
      case 3:
        return <ProfilePage />;
      default:
        return <FilesPage />;
    }
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ maxWidth: 480, margin: '0 auto', height: '100vh', position: 'relative', bgcolor: '#fafafa' }}>
        {renderPage()}
        <BottomNav value={currentTab} onChange={setCurrentTab} />
      </Box>
    </ThemeProvider>
  );
}