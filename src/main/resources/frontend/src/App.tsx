import { AuthProvider } from './contexts/AuthContext';
import Layout from './components/Layout';

function App() {
  return (
    <AuthProvider>
      <Layout />
    </AuthProvider>
  );
}

export default App;



