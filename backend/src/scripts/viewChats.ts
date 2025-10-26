import express from 'express';
import mongoose from 'mongoose';
import { config } from 'dotenv';
import { Message } from '../models/message.model';

config();

const app = express();
const PORT = 8082;

function escapeHtml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

async function startViewer() {
  try {
    console.log('🔌 Connecting to MongoDB...');
    await mongoose.connect(process.env.MONGODB_URI!);
    console.log('✅ Connected to MongoDB\n');

    const chatsCollection = mongoose.connection.collection('chats');

    app.get('/', async (req, res) => {
      try {
        const page = parseInt(req.query.page as string) || 1;
        const limit = parseInt(req.query.limit as string) || 50;
        const skip = (page - 1) * limit;

        const totalChats = await chatsCollection.countDocuments();
        const chats = await chatsCollection.find()
          .skip(skip)
          .limit(limit)
          .toArray();

        const totalPages = Math.ceil(totalChats / limit) || 1;

        const html = `
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <title>MongoDB Chats Viewer</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; padding: 20px; }
    .container { max-width: 1400px; margin: 0 auto; background: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); padding: 20px; }
    h1 { color: #333; margin-bottom: 10px; }
    .stats { background: #e8f5e9; padding: 15px; border-radius: 4px; margin-bottom: 20px; display: flex; gap: 30px; flex-wrap: wrap; }
    .stat-item { font-size: 14px; color: #2e7d32; }
    .stat-item strong { font-size: 20px; display: block; color: #1b5e20; }
    .controls { margin-bottom: 20px; display: flex; gap: 10px; align-items: center; }
    .controls select, .controls button { padding: 8px 12px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }
    .controls button { background: #2e7d32; color: white; cursor: pointer; border: none; }
    .controls button:hover { background: #1b5e20; }
    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 14px; }
    th { background: #2e7d32; color: white; padding: 12px 8px; text-align: left; font-weight: 600; position: sticky; top: 0; }
    td { padding: 10px 8px; border-bottom: 1px solid #e0e0e0; vertical-align: top; }
    tr:hover { background: #f5f5f5; }
    .pill { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: bold; }
    .pill-dm { background: #1976d2; color: white; }
    .pill-group { background: #6a1b9a; color: white; }
    .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; font-size: 12px; }
    .pagination { display: flex; gap: 10px; justify-content: center; align-items: center; margin-top: 20px; }
    .pagination a, .pagination span { padding: 8px 12px; border: 1px solid #ddd; border-radius: 4px; text-decoration: none; color: #2e7d32; }
    .pagination span { background: #2e7d32; color: white; border-color: #2e7d32; }
    .pagination a:hover { background: #e8f5e9; }
    .nowrap { white-space: nowrap; }
  </style>
</head>
<body>
  <div class="container">
    <h1>💬 MongoDB Chats</h1>

    <div class="stats">
      <div class="stat-item"><strong>${totalChats}</strong>Total Chats</div>
      <div class="stat-item"><strong>${page}</strong>Current Page</div>
      <div class="stat-item"><strong>${totalPages}</strong>Total Pages</div>
      <div class="stat-item"><strong>${chats.length}</strong>Showing on this page</div>
    </div>

    <div class="controls">
      <label>Show per page:</label>
      <select id="limitSelect" onchange="changeLimit(this.value)">
        <option value="25" ${limit === 25 ? 'selected' : ''}>25</option>
        <option value="50" ${limit === 50 ? 'selected' : ''}>50</option>
        <option value="100" ${limit === 100 ? 'selected' : ''}>100</option>
        <option value="500" ${limit === 500 ? 'selected' : ''}>500</option>
        <option value="${totalChats}">All (${totalChats})</option>
      </select>
      <button onclick="window.location.href='/?limit=${totalChats}'">Show All Chats</button>
    </div>

    <table>
      <thead>
        <tr>
          <th>#</th>
          <th>Chat ID</th>
          <th>Type</th>
          <th>Name</th>
          <th>Participants</th>
          <th>Last Message</th>
          <th class="nowrap">Last At</th>
          <th class="nowrap">Updated</th>
          <th class="nowrap">Created</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        ${chats.map((c: any, idx: number) => {
          const typePill = c.isGroup ? '<span class="pill pill-group">Group</span>' : '<span class="pill pill-dm">Direct</span>';
          const participants = (c.participants || []).map((p: any) => escapeHtml(p?.name || String(p?._id || 'unknown'))).join(', ');
          const participantNames = (c.participants || []).map((p: any) => escapeHtml(p?.name || String(p?._id || 'unknown')));
          const lastMsg = c.lastMessage ? escapeHtml(c.lastMessage.content || '') : '';
          const lastAt = c.lastMessageAt ? new Date(c.lastMessageAt).toLocaleString() : '-';
          const updatedAt = c.updatedAt ? new Date(c.updatedAt).toLocaleString() : '-';
          const createdAt = c.createdAt ? new Date(c.createdAt).toLocaleString() : '-';
          const cid = String(c._id || c.id);
          return `
            <tr>
              <td>${skip + idx + 1}</td>
              <td class="mono">${cid}</td>
              <td>${typePill}</td>
              <td>${escapeHtml(c.name || '-')}</td>
              <td>${participants || '-'}</td>
              <td>${lastMsg || '-'}</td>
              <td class="nowrap">${lastAt}</td>
              <td class="nowrap">${updatedAt}</td>
              <td class="nowrap">${createdAt}</td>
              <td><a href="/messages/${cid}">View Messages</a></td>
            </tr>
          `;
        }).join('')}
      </tbody>
    </table>

    <div class="pagination">
      ${page > 1 ? `<a href="?page=${page - 1}&limit=${limit}">← Previous</a>` : ''}
      ${Array.from({ length: Math.min(10, totalPages) }, (_, i) => {
          const pageNum = i + 1;
          if (pageNum === page) return `<span>${pageNum}</span>`;
          return `<a href="?page=${pageNum}&limit=${limit}">${pageNum}</a>`;
        }).join('')}
      ${totalPages > 10 && page < totalPages - 5 ? '...' : ''}
      ${page < totalPages ? `<a href="?page=${page + 1}&limit=${limit}">Next →</a>` : ''}
    </div>
  </div>

  <script>
    function changeLimit(value) { window.location.href = '?page=1&limit=' + value; }
  </script>
</body>
</html>`;

        res.setHeader('Content-Type', 'text/html; charset=utf-8');
        res.send(html);
      } catch (error: any) {
        res.status(500).send(`Error: ${error?.message || String(error)}`);
      }
    });

    app.get('/messages/:chatId', async (req, res) => {
      try {
        const { chatId } = req.params;
        const limit = Math.max(1, Math.min(1500, parseInt(req.query.limit as string) || 200));

        if (!mongoose.isValidObjectId(chatId)) {
          res.status(400).send('Invalid chatId');
          return;
        }

        const objectId = new mongoose.Types.ObjectId(chatId);
        const chatsCollection = mongoose.connection.collection('chats');
        const messagesCollection = mongoose.connection.collection('messages');
        const usersCollection = mongoose.connection.collection('users');

        const chat = await chatsCollection.findOne({ _id: objectId });
        if (!chat) {
          res.status(404).send('Chat not found');
          return;
        }

        const messages = await messagesCollection
          .find({ chat: objectId })
          .sort({ createdAt: 1 })
          .limit(limit)
          .toArray();

        const senderIds = Array.from(
          new Set(
            messages
              .map((m: any) => m.sender)
              .filter((id: any) => !!id)
              .map((id: any) => (typeof id === 'string' ? new mongoose.Types.ObjectId(id) : id))
          )
        );

        const participantIds = (chat.participants || []).map((id: any) =>
          typeof id === 'string' ? new mongoose.Types.ObjectId(id) : id
        );

        const users = await usersCollection
          .find({ _id: { $in: [...senderIds, ...participantIds] } })
          .project({ name: 1 })
          .toArray();

        const userMap = new Map<string, string>();
        for (const u of users) {
          userMap.set(String(u._id), u.name || String(u._id));
        }

        const isGroup = Array.isArray(chat.participants) && chat.participants.length >= 3;
        const participantNames = (chat.participants || [])
          .map((p: any) => userMap.get(String(p)) || String(p))
          .map((n: string) => escapeHtml(n));

        const html = `
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <title>Messages for Chat ${escapeHtml(String(chatId))}</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; padding: 20px; }
    .container { max-width: 1200px; margin: 0 auto; background: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); padding: 20px; }
    h1 { color: #333; margin-bottom: 10px; }
    .meta { margin-bottom: 18px; color: #555; }
    .pill { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: bold; }
    .pill-dm { background: #1976d2; color: white; }
    .pill-group { background: #6a1b9a; color: white; }
    .controls { margin: 10px 0 16px; display: flex; gap: 10px; align-items: center; }
    table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 14px; }
    th { background: #2e7d32; color: white; padding: 12px 8px; text-align: left; font-weight: 600; position: sticky; top: 0; }
    td { padding: 10px 8px; border-bottom: 1px solid #e0e0e0; vertical-align: top; }
    tr:hover { background: #f5f5f5; }
    .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; font-size: 12px; }
    .nowrap { white-space: nowrap; }
    a.button { display: inline-block; padding: 8px 12px; border-radius: 4px; background: #2e7d32; color: #fff; text-decoration: none; }
  </style>
  <script>
    function changeLimit(value) { window.location.search = '?limit=' + value; }
  </script>
  </head>
  <body>
    <div class="container">
      <a class="button" href="/">← Back to Chats</a>
      <h1>Chat ${escapeHtml(String(chat._id))} Messages</h1>
      <div class="meta">
        Type: ${isGroup ? '<span class="pill pill-group">Group</span>' : '<span class="pill pill-dm">Direct</span>'}
        &nbsp;•&nbsp; Name: ${escapeHtml(chat.name || '-')}
        &nbsp;•&nbsp; Participants: ${participantNames.join(', ') || '-'}
      </div>
      <div class="controls">
        <label>Show first</label>
        <select onchange="changeLimit(this.value)">
          <option value="50" ${limit === 50 ? 'selected' : ''}>50</option>
          <option value="100" ${limit === 100 ? 'selected' : ''}>100</option>
          <option value="200" ${limit === 200 ? 'selected' : ''}>200</option>
          <option value="500" ${limit === 500 ? 'selected' : ''}>500</option>
        </select>
        <span>messages (oldest → newest)</span>
      </div>

      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Message ID</th>
            <th>Sender</th>
            <th>Content</th>
            <th class="nowrap">Created At</th>
            <th class="nowrap">Updated At</th>
          </tr>
        </thead>
        <tbody>
          ${messages
            .map((m: any, idx: number) => {
              const mid = String(m._id || m.id);
              const senderName = userMap.get(String(m.sender)) || String(m.sender);
              const content = escapeHtml(m.content || '');
              const createdAt = m.createdAt ? new Date(m.createdAt).toLocaleString() : '-';
              const updatedAt = m.updatedAt ? new Date(m.updatedAt).toLocaleString() : '-';
              return `
                <tr>
                  <td>${idx + 1}</td>
                  <td class="mono">${mid}</td>
                  <td>${escapeHtml(senderName)}</td>
                  <td>${content || '-'}</td>
                  <td class="nowrap">${createdAt}</td>
                  <td class="nowrap">${updatedAt}</td>
                </tr>
              `;
            })
            .join('')}
        </tbody>
      </table>
      ${messages.length === 0 ? '<p>No messages found for this chat.</p>' : ''}
    </div>
  </body>
</html>`;

        res.setHeader('Content-Type', 'text/html; charset=utf-8');
        res.send(html);
      } catch (error: any) {
        res.status(500).send(`Error: ${error?.message || String(error)}`);
      }
    });

    app.listen(PORT, () => {
        console.log('🌐 MongoDB Chats Viewer is running!');
        console.log(`📱 Open your browser: http://localhost:${PORT}`);
        console.log('🛑 Press Ctrl+C to stop\n');
      });
  }
  catch (error: any) {
    console.error('❌ Error:', error);
    process.exit(1);
  }
}

startViewer();


