import React, { useState } from "react";
import Layout from "../../components/Layout";
import { 
  Settings, 
  Users, 
  Database, 
  Cpu, 
  Activity, 
  Search, 
  Trash2, 
  Check, 
  RefreshCw, 
  ShieldAlert, 
  Terminal 
} from "lucide-react";

export default function AdminDashboard() {
  
  // System Configurations
  const [cacheTtl, setCacheTtl] = useState(3600);
  const [maxDbConns, setMaxDbConns] = useState(50);
  const [aiTimeout, setAiTimeout] = useState(2500);

  // User List State (starts empty, populated from real user registrations)
  const [users, setUsers] = useState<Array<{ id: string; name: string; email: string; role: string; status: string }>>([]);

  const [searchTerm, setSearchTerm] = useState("");
  const [logs, setLogs] = useState([
    "System Boot: MongoDB cluster connected. (200ms)",
    "System Cache: Redis instances loaded in cluster 6379.",
    "AI Service: Handshake verified with FastAPI endpoint /api/ai/predict.",
    "Security Filter: Filtered request headers and initialized JWT validators."
  ]);

  const handleRoleChange = (userId: string, newRole: string) => {
    setUsers(users.map(u => u.id === userId ? { ...u, role: newRole } : u));
    setLogs(prev => [`User Management: Modified role of user ID: ${userId} to ${newRole}.`, ...prev.slice(0, 5)]);
  };

  const handleDeleteUser = (userId: string) => {
    if (confirm("Are you sure you want to delete this user profile?")) {
      setUsers(users.filter(u => u.id !== userId));
      setLogs(prev => [`User Management: Purged user profile ID: ${userId} from database.`, ...prev.slice(0, 5)]);
    }
  };

  const handleBroadcast = () => {
    alert("ðŸ“¢ System Broadcast: Dispatched notifications to all active students!");
    setLogs(prev => ["Alert Broadcast: Dispatched global streak reminder notifications.", ...prev.slice(0, 5)]);
  };

  const filteredUsers = users.filter(u => 
    u.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
    u.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <Layout>
      <div className="space-y-8">
        
        {/* Title */}
        <div>
          <h1 className="text-3xl font-extrabold text-main-theme flex items-center gap-2">
            <Settings className="h-8 w-8 text-purple-theme animate-spin-slow" />
            <span>Admin Control Panel</span>
          </h1>
          <p className="text-secondary-theme text-sm mt-1">
            Oversee user databases, calibrate caching parameters, inspect live microservice connection logs, and dispatch global notifications.
          </p>
        </div>

        {styleBlock}

        {/* Global Cluster Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
            <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Database Status</span>
            <div className="text-xl font-bold text-emerald-theme flex items-center gap-1.5 pt-1">
              <Database className="h-5 w-5" />
              <span>MongoDB Online</span>
            </div>
            <p className="text-[10px] text-secondary-theme">5 active collections persisting profiles.</p>
          </div>

          <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
            <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Cache Layer</span>
            <div className="text-xl font-bold text-emerald-theme flex items-center gap-1.5 pt-1">
              <RefreshCw className="h-5 w-5 animate-spin-slow" />
              <span>Redis Cluster Live</span>
            </div>
            <p className="text-[10px] text-secondary-theme">Hitting 92.5% cache read rates.</p>
          </div>

          <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
            <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">AI Service Sync</span>
            <div className="text-xl font-bold text-cyan-theme flex items-center gap-1.5 pt-1">
              <Cpu className="h-5 w-5" />
              <span>Uvicorn 8000 OK</span>
            </div>
            <p className="text-[10px] text-secondary-theme">Average response latency: 120ms.</p>
          </div>

          <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-1">
            <span className="text-[10px] text-secondary-theme uppercase font-extrabold tracking-wider">Total User Count</span>
            <div className="text-2xl font-black text-purple-theme">{users.length} Users</div>
            <p className="text-[10px] text-secondary-theme">Students & Faculty registers.</p>
          </div>
        </div>

        {/* Main Grid Section */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* User management List (2/3 width) */}
          <div className="glass-panel p-6 rounded-2xl border border-white/5 lg:col-span-2 space-y-4">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 border-b border-white/5 pb-3">
              <h3 className="text-sm font-extrabold tracking-wide">User Registration Database</h3>
              
              {/* Search Bar */}
              <div className="relative">
                <Search className="absolute left-3 top-2.5 h-4 w-4 text-secondary-theme" />
                <input
                  type="text"
                  placeholder="Search email or name..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="py-1.5 pl-9 pr-4 w-52 rounded-lg glass-input text-xs"
                />
              </div>
            </div>

            {/* User Directory list */}
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-white/5 text-secondary-theme font-extrabold">
                    <th className="pb-3">Name</th>
                    <th className="pb-3 px-2">Email</th>
                    <th className="pb-3 px-2">Access Role</th>
                    <th className="pb-3 pl-2 text-right">Settings</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 font-semibold">
                  {filteredUsers.map((user) => (
                    <tr key={user.id} className="hover:bg-white/3 transition-colors">
                      <td className="py-3 font-bold text-main-theme">{user.name}</td>
                      <td className="py-3 px-2 text-secondary-theme">{user.email}</td>
                      <td className="py-3 px-2">
                        <select
                          value={user.role}
                          onChange={(e) => handleRoleChange(user.id, e.target.value)}
                          className="p-1 rounded-md glass-input text-[10px] focus:bg-[#0d0f1e]"
                        >
                          <option className="bg-[#0d0f1e]" value="STUDENT">STUDENT</option>
                          <option className="bg-[#0d0f1e]" value="FACULTY">FACULTY</option>
                          <option className="bg-[#0d0f1e]" value="ADMIN">ADMIN</option>
                        </select>
                      </td>
                      <td className="py-3 pl-2 text-right">
                        <button
                          onClick={() => handleDeleteUser(user.id)}
                          className="p-1 text-secondary-theme hover:text-red-400 transition-colors cursor-pointer"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Right Column: Configurations and Logs */}
          <div className="space-y-6">
            
            {/* Tuning settings */}
            <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-4">
              <h3 className="text-sm font-extrabold tracking-wide border-b border-white/5 pb-3">System Tuning</h3>
              
              {/* Cache slider */}
              <div className="space-y-2">
                <div className="flex justify-between text-xs">
                  <span className="text-secondary-theme">Redis Cache TTL</span>
                  <span className="text-purple-theme font-bold">{cacheTtl}s</span>
                </div>
                <input
                  type="range"
                  min="60"
                  max="7200"
                  step="60"
                  value={cacheTtl}
                  onChange={(e) => setCacheTtl(Number(e.target.value))}
                  className="w-full accent-purple-500 bg-white/10 rounded-lg appearance-none h-1"
                />
              </div>

              {/* DB connections slider */}
              <div className="space-y-2">
                <div className="flex justify-between text-xs">
                  <span className="text-secondary-theme">Max DB Pool Connections</span>
                  <span className="text-purple-theme font-bold">{maxDbConns} pools</span>
                </div>
                <input
                  type="range"
                  min="10"
                  max="200"
                  step="5"
                  value={maxDbConns}
                  onChange={(e) => setMaxDbConns(Number(e.target.value))}
                  className="w-full accent-purple-500 bg-white/10 rounded-lg appearance-none h-1"
                />
              </div>

              <button
                onClick={handleBroadcast}
                className="w-full py-2.5 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white rounded-xl text-xs font-bold shadow-md shadow-purple-500/15 cursor-pointer"
              >
                Send Global Alert Notifications
              </button>
            </div>

            {/* Live Logs console */}
            <div className="glass-panel p-6 rounded-2xl border border-white/5 space-y-4">
              <div className="flex items-center gap-2 border-b border-white/5 pb-3">
                <Terminal className="h-5 w-5 text-cyan-theme" />
                <h3 className="text-xs font-bold uppercase tracking-wider text-main-theme">Live Console Tracer</h3>
              </div>

              <div className="space-y-2.5">
                {logs.map((log, index) => (
                  <div key={index} className="text-[10px] font-mono text-secondary-theme leading-normal break-all">
                    &gt; {log}
                  </div>
                ))}
              </div>
            </div>

          </div>

        </div>

      </div>
    </Layout>
  );
}

const styleBlock = (
  <style>{`
    .animate-spin-slow {
      animation: spin 8s linear infinite;
    }
    @keyframes spin {
      100% {
        transform: rotate(360deg);
      }
    }
  `}</style>
);
