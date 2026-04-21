import { BottomNavigation, BottomNavigationAction, Paper } from '@mui/material';
import { Folder, Group, SmartToy, Person } from '@mui/icons-material';

interface BottomNavProps {
  value: number;
  onChange: (value: number) => void;
}

export default function BottomNav({ value, onChange }: BottomNavProps) {
  return (
    <Paper sx={{ position: 'fixed', bottom: 0, left: 0, right: 0 }} elevation={3}>
      <BottomNavigation
        value={value}
        onChange={(_, newValue) => onChange(newValue)}
        showLabels
      >
        <BottomNavigationAction label="文件" icon={<Folder />} />
        <BottomNavigationAction label="群组" icon={<Group />} />
        <BottomNavigationAction label="Agent" icon={<SmartToy />} />
        <BottomNavigationAction label="我的" icon={<Person />} />
      </BottomNavigation>
    </Paper>
  );
}
