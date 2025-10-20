import express from 'express';
import mongoose from 'mongoose';
import { config } from 'dotenv';

config();

const app = express();
const PORT = 8081;

async function startViewer() {
  try {
    console.log('🔌 Connecting to MongoDB...');
    await mongoose.connect(process.env.MONGODB_URI!);
    console.log('✅ Connected to MongoDB\n');

    const usersCollection = mongoose.connection.collection('users');

    app.get('/', async (req, res) => {
      try {
        const page = parseInt(req.query.page as string) || 1;
        const limit = parseInt(req.query.limit as string) || 50;
        const skip = (page - 1) * limit;

        const totalUsers = await usersCollection.countDocuments();
        const users = await usersCollection
          .find({})
          .skip(skip)
          .limit(limit)
          .toArray();

        const totalPages = Math.ceil(totalUsers / limit);

        const html = `
<!DOCTYPE html>
<html>
<head>
  <title>MongoDB Users Viewer</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: #f5f5f5;
      padding: 20px;
    }
    .container {
      max-width: 1400px;
      margin: 0 auto;
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      padding: 20px;
    }
    h1 {
      color: #333;
      margin-bottom: 10px;
    }
    .stats {
      background: #e3f2fd;
      padding: 15px;
      border-radius: 4px;
      margin-bottom: 20px;
      display: flex;
      gap: 30px;
      flex-wrap: wrap;
    }
    .stat-item {
      font-size: 14px;
      color: #1976d2;
    }
    .stat-item strong {
      font-size: 20px;
      display: block;
      color: #0d47a1;
    }
    .controls {
      margin-bottom: 20px;
      display: flex;
      gap: 10px;
      align-items: center;
    }
    .controls select, .controls button {
      padding: 8px 12px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 14px;
    }
    .controls button {
      background: #1976d2;
      color: white;
      cursor: pointer;
      border: none;
    }
    .controls button:hover {
      background: #1565c0;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      margin-bottom: 20px;
      font-size: 14px;
    }
    th {
      background: #1976d2;
      color: white;
      padding: 12px 8px;
      text-align: left;
      font-weight: 600;
      position: sticky;
      top: 0;
    }
    td {
      padding: 10px 8px;
      border-bottom: 1px solid #e0e0e0;
    }
    tr:hover {
      background: #f5f5f5;
    }
    .pagination {
      display: flex;
      gap: 10px;
      justify-content: center;
      align-items: center;
      margin-top: 20px;
    }
    .pagination a, .pagination span {
      padding: 8px 12px;
      border: 1px solid #ddd;
      border-radius: 4px;
      text-decoration: none;
      color: #1976d2;
    }
    .pagination span {
      background: #1976d2;
      color: white;
      border-color: #1976d2;
    }
    .pagination a:hover {
      background: #e3f2fd;
    }
    .ready-badge {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: bold;
    }
    .ready-yes {
      background: #4caf50;
      color: white;
    }
    .ready-no {
      background: #f44336;
      color: white;
    }
  </style>
</head>
<body>
  <div class="container">
    <h1>🗄️ MongoDB Users Database</h1>
    
    <div class="stats">
      <div class="stat-item">
        <strong>${totalUsers}</strong>
        Total Users
      </div>
      <div class="stat-item">
        <strong>${page}</strong>
        Current Page
      </div>
      <div class="stat-item">
        <strong>${totalPages}</strong>
        Total Pages
      </div>
      <div class="stat-item">
        <strong>${users.length}</strong>
        Showing on this page
      </div>
    </div>

    <div class="controls">
      <label>Show per page:</label>
      <select id="limitSelect" onchange="changeLimit(this.value)">
        <option value="25" ${limit === 25 ? 'selected' : ''}>25</option>
        <option value="50" ${limit === 50 ? 'selected' : ''}>50</option>
        <option value="100" ${limit === 100 ? 'selected' : ''}>100</option>
        <option value="500" ${limit === 500 ? 'selected' : ''}>500</option>
        <option value="${totalUsers}">All (${totalUsers})</option>
      </select>
      <button onclick="window.location.href='/?limit=${totalUsers}'">Show All Users</button>
    </div>

    <table>
      <thead>
        <tr>
          <th>#</th>
          <th>Name</th>
          <th>Email</th>
          <th>Age</th>
          <th>Skill Level</th>
          <th>Coordinates</th>
          <th>Location</th>
          <th>Bio</th>
          <th>Ready</th>
        </tr>
      </thead>
      <tbody>
        ${users.map((user: any, index: number) => {
          const isReady = user.age && user.skillLevel && user.latitude && user.longitude;
          const locationText = user.location || '-';
          
          return `
            <tr>
              <td>${skip + index + 1}</td>
              <td><strong>${user.name}</strong></td>
              <td style="font-size: 12px;">${user.email}</td>
              <td>${user.age || '-'}</td>
              <td>${user.skillLevel || '-'}</td>
              <td style="font-size: 11px;">${user.latitude ? user.latitude.toFixed(4) : '-'}, ${user.longitude ? user.longitude.toFixed(4) : '-'}</td>
              <td style="font-size: 12px;">${locationText}</td>
              <td style="font-size: 12px; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${user.bio || '-'}</td>
              <td><span class="ready-badge ${isReady ? 'ready-yes' : 'ready-no'}">${isReady ? 'YES' : 'NO'}</span></td>
            </tr>
          `;
        }).join('')}
      </tbody>
    </table>

    <div class="pagination">
      ${page > 1 ? `<a href="?page=${page - 1}&limit=${limit}">← Previous</a>` : ''}
      
      ${Array.from({ length: Math.min(10, totalPages) }, (_, i) => {
        const pageNum = i + 1;
        if (pageNum === page) {
          return `<span>${pageNum}</span>`;
        }
        return `<a href="?page=${pageNum}&limit=${limit}">${pageNum}</a>`;
      }).join('')}
      
      ${totalPages > 10 && page < totalPages - 5 ? '...' : ''}
      
      ${page < totalPages ? `<a href="?page=${page + 1}&limit=${limit}">Next →</a>` : ''}
    </div>
  </div>

  <script>
    function changeLimit(value) {
      window.location.href = '?page=1&limit=' + value;
    }
  </script>
</body>
</html>
        `;

        res.send(html);
      } catch (error) {
        res.status(500).send(`Error: ${error}`);
      }
    });

    app.listen(PORT, () => {
      console.log('🌐 MongoDB User Viewer is running!');
      console.log(`📱 Open your browser: http://localhost:${PORT}`);
      console.log(`📊 Total users in database: ${usersCollection ? 'Loading...' : 'Unknown'}`);
      console.log('🛑 Press Ctrl+C to stop\n');
    });

  } catch (error) {
    console.error('❌ Error starting viewer:', error);
    process.exit(1);
  }
}

startViewer();



