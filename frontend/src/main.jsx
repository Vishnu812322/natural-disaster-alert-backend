import React, { useEffect, useMemo, useState } from 'react'

import { createRoot } from 'react-dom/client'

import {

  MapContainer, TileLayer, Circle, CircleMarker, Popup, useMap

} from 'react-leaflet'

import 'leaflet/dist/leaflet.css'

import './style.css'



const API = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const DEFAULT_CENTER = [12.9716, 77.5946]



async function api(path, opts = {}) {

  const token = localStorage.getItem('token')

  const headers = { 'Content-Type': 'application/json', ...(opts.headers || {}) }

  if (token) headers.Authorization = `Bearer ${token}`

  const response = await fetch(API + path, { ...opts, headers })

  if (!response.ok) {

    let message = await response.text()

    try { message = JSON.parse(message).message || message } catch {}

    throw new Error(message || `Request failed: ${response.status}`)

  }

  return response.status === 204 ? null : response.json()

}



function Header({ admin = false, onLogout }) {

  return (

    <header className="topbar">

      <a className="brand" href="#/"><span className="brand-icon">🚨</span> Disaster Alert</a>

      <nav>

        <a href="#/">Public Alerts</a>

        {admin && <a href="#/admin">Admin</a>}

        {admin && <button className="link-button" onClick={onLogout}>Logout</button>}

      </nav>

    </header>

  )

}



function SeverityBadge({ severity }) {

  return <span className={`severity severity-${String(severity || '').toLowerCase()}`}>{severity || 'UNKNOWN'}</span>

}



function FitBounds({ alerts }) {

  const map = useMap()

  useEffect(() => {

    const points = alerts

      .filter(a => Number.isFinite(a.centerLatitude) && Number.isFinite(a.centerLongitude))

      .map(a => [a.centerLatitude, a.centerLongitude])

    if (points.length === 1) map.setView(points[0], 9)

    if (points.length > 1) map.fitBounds(points, { padding: [30, 30] })

  }, [alerts, map])

  return null

}



function AlertMap({ alerts, publicMode = false }) {

  return (

    <div className="map-wrap">

      <MapContainer center={DEFAULT_CENTER} zoom={7} scrollWheelZoom className="map">

        <TileLayer

          attribution='&copy; OpenStreetMap contributors'

          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"

        />

        <FitBounds alerts={alerts} />

        {alerts.map(alert => {

          if (!Number.isFinite(alert.centerLatitude) || !Number.isFinite(alert.centerLongitude)) return null

          const position = [alert.centerLatitude, alert.centerLongitude]

          const radius = Math.max(500, Number(alert.radiusKm || 1) * 1000)

          return (

            <React.Fragment key={alert.id}>

              <Circle

                center={position}

                radius={radius}

                pathOptions={{

                  color: alert.severity === 'EXTREME' ? '#991b1b' :

                    alert.severity === 'SEVERE' ? '#dc2626' : '#d97706',

                  fillOpacity: 0.18

                }}

              />

              <CircleMarker

                center={position}

                radius={8}

                pathOptions={{ color: '#ffffff', weight: 2, fillOpacity: 1,

                  fillColor: alert.severity === 'EXTREME' ? '#991b1b' :

                    alert.severity === 'SEVERE' ? '#dc2626' : '#d97706' }}

              >

                <Popup>

                  <strong>{alert.title}</strong><br/>

                  <SeverityBadge severity={alert.severity} /><br/>

                  {alert.areaText || 'Affected area'}<br/>

                  {alert.testAlert && <em>TEST ALERT</em>}

                </Popup>

              </CircleMarker>

            </React.Fragment>

          )

        })}

      </MapContainer>

      {publicMode && <div className="map-legend">Map shows alerts with geographic coordinates supplied by the alert source.</div>}

    </div>

  )

}



function Public() {

  const [alerts, setAlerts] = useState([])

  const [error, setError] = useState('')

  const load = () => api('/public/alerts').then(setAlerts).catch(e => setError(e.message))

  useEffect(() => { load(); const t = setInterval(load, 30000); return () => clearInterval(t) }, [])



  return (

    <>

      <Header />

      <main>

        <section className="hero">

          <div>

            <span className="pill">EARLY WARNING & PUBLIC SAFETY</span>

            <h1>Natural Disaster Alert Center</h1>

            <p>Monitor active warnings, affected areas and safety instructions from configured authoritative sources.</p>

          </div>

          <div className="hero-stat"><b>{alerts.length}</b><span>Active alerts</span></div>

        </section>

        {error && <div className="error-box">{error}</div>}

        {alerts.length > 0 && <AlertMap alerts={alerts} publicMode />}

        <div className="section-heading"><div><span className="eyebrow">LIVE</span><h2>Active alerts</h2></div><span className="refresh">Updates every 30 seconds</span></div>

        {alerts.length === 0 ? (

          <div className="empty-card"><span>✓</span><h3>No active alerts</h3><p>No active alerts are currently available from this system.</p></div>

        ) : alerts.map(a => (

          <article className="alert-card" key={a.id}>

            <div className="alert-card-main">

              <div className="alert-title-row"><SeverityBadge severity={a.severity}/>{a.testAlert && <span className="test-badge">TEST</span>}</div>

              <h3>{a.title}</h3>

              <p>{a.description}</p>

              <div className="meta-row"><span>🌐 {a.disasterType}</span><span>Source: {a.source}</span><span>📍 {a.areaText || 'Geographic area supplied by source'}</span></div>

            </div>

            <div className="instructions">

              <span className="eyebrow">SAFETY INSTRUCTIONS</span>

              <p>{a.instructions || 'Follow official local-authority instructions.'}</p>

            </div>

          </article>

        ))}

      </main>

    </>

  )

}



const blankForm = {

  title: '', description: '', disasterType: 'FLOOD', severity: 'HIGH',

  source: 'DEVELOPMENT_TEST', sourceAlertId: '', centerLatitude: '12.9716',

  centerLongitude: '77.5946', radiusKm: '10', areaText: '',

  instructions: '', testAlert: true

}



function AdminLogin({ onLogin }) {

  const [email, setEmail] = useState('admin@example.com')

  const [password, setPassword] = useState('Admin@12345')

  const [otp, setOtp] = useState('')

  const [step, setStep] = useState(1)

  const [message, setMessage] = useState('')

  const [busy, setBusy] = useState(false)



  const login = async () => {

    setBusy(true); setMessage('')

    try { await api('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }); setStep(2); setMessage('OTP generated. Check the Spring Boot console in development mode.') }

    catch (e) { setMessage(e.message) } finally { setBusy(false) }

  }

  const verify = async () => {

    setBusy(true); setMessage('')

    try { const r = await api('/auth/verify-otp', { method: 'POST', body: JSON.stringify({ email, otp }) }); localStorage.setItem('token', r.token); onLogin() }

    catch (e) { setMessage(e.message) } finally { setBusy(false) }

  }



  return (

    <main className="login-page">

      <div className="login-card">

        <div className="login-logo">🚨</div>

        <span className="eyebrow">SECURE CONTROL CENTER</span>

        <h1>{step === 1 ? 'Admin Sign in' : 'Verify OTP'}</h1>

        <p className="muted">{step === 1 ? 'Authorized personnel only.' : 'Enter the one-time code shown by the development OTP provider.'}</p>

        {step === 1 ? <>

          <label>Email<input value={email} onChange={e => setEmail(e.target.value)} autoComplete="username"/></label>

          <label>Password<input type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete="current-password"/></label>

          <button className="primary full" onClick={login} disabled={busy}>{busy ? 'Sending…' : 'Continue'}</button>

        </> : <>

          <label>One-time password<input value={otp} onChange={e => setOtp(e.target.value)} inputMode="numeric" maxLength="6" placeholder="6 digits"/></label>

          <button className="primary full" onClick={verify} disabled={busy}>{busy ? 'Verifying…' : 'Verify & Enter'}</button>

          <button className="secondary full" onClick={() => {setStep(1);setMessage('')}}>Back</button>

        </>}

        {message && <div className="notice">{message}</div>}

      </div>

    </main>

  )

}



function Admin() {

  const [authenticated, setAuthenticated] = useState(!!localStorage.getItem('token'))

  const [summary, setSummary] = useState(null)

  const [alerts, setAlerts] = useState([])

  const [view, setView] = useState('dashboard')

  const [form, setForm] = useState(blankForm)

  const [message, setMessage] = useState('')

  const [error, setError] = useState('')

  const [busy, setBusy] = useState(false)



  const load = async () => {

    try {

      setError('')

      const [s, a] = await Promise.all([api('/dashboard/summary'), api('/alerts')])

      setSummary(s); setAlerts(a)

    } catch (e) { setError(e.message) }

  }

  useEffect(() => { if (authenticated) load() }, [authenticated])



  const logout = () => { localStorage.removeItem('token'); setAuthenticated(false) }



  const createAlert = async e => {

    e.preventDefault(); setBusy(true); setMessage(''); setError('')

    try {

      const payload = {

        ...form,

        centerLatitude: Number(form.centerLatitude),

        centerLongitude: Number(form.centerLongitude),

        radiusKm: Number(form.radiusKm),

        testAlert: true

      }

      const created = await api('/alerts', { method: 'POST', body: JSON.stringify(payload) })

      setMessage(`Test alert #${created.id} created successfully.`)

      setForm(blankForm); setView('alerts'); await load()

    } catch (e) { setError(e.message) } finally { setBusy(false) }

  }



  const resolve = async id => {

    if (!confirm('Resolve this alert?')) return

    try { await api(`/alerts/${id}/resolve`, { method: 'POST' }); setMessage(`Alert #${id} resolved.`); load() }

    catch (e) { setError(e.message) }

  }



  if (!authenticated) return <AdminLogin onLogin={() => setAuthenticated(true)} />



  const active = alerts.filter(a => a.status === 'ACTIVE')

  const severe = active.filter(a => ['SEVERE', 'EXTREME'].includes(a.severity)).length

  const tests = active.filter(a => a.testAlert).length



  return (

    <div className="admin-shell">

      <Header admin onLogout={logout}/>

      <div className="admin-body">

        <aside className="sidebar">

          <div className="side-label">CONTROL CENTER</div>

          {[

            ['dashboard','▦','Dashboard'],['alerts','⚠','Alerts'],['map','⌖','Live Map'],

            ['create','＋','Create Alert'],['notifications','◉','Notifications'],['users','♙','Users & Devices'],

            ['sources','⇄','Data Sources'],['safety','✚','Emergency Services'],['audit','▤','Audit Logs']

          ].map(([key,icon,label]) => (

            <button key={key} className={view === key ? 'side-item active' : 'side-item'} onClick={() => setView(key)}>

              <span>{icon}</span>{label}

            </button>

          ))}

          <div className="side-note"><b>System status</b><span><i className="online-dot"/> Backend connected</span><small>Development integrations are clearly separated from production providers.</small></div>

        </aside>



        <section className="admin-content">

          <div className="page-title">

            <div><span className="eyebrow">EMERGENCY OPERATIONS</span><h1>{view === 'dashboard' ? 'Control Center' : view === 'create' ? 'Create Alert' : view === 'map' ? 'Live Disaster Map' : view === 'sources' ? 'Authorized Data Sources' : view.replace(/^\w/, c => c.toUpperCase())}</h1></div>

            <button className="primary" onClick={load}>↻ Refresh</button>

          </div>



          {message && <div className="success-box">{message}</div>}

          {error && <div className="error-box">{error}</div>}



          {view === 'dashboard' && <Dashboard summary={summary} active={active} severe={severe} tests={tests} onAlerts={()=>setView('alerts')} onCreate={()=>setView('create')} />}

          {view === 'alerts' && <Alerts alerts={alerts} onResolve={resolve} onCreate={()=>setView('create')}/>}

          {view === 'map' && <><div className="panel"><AlertMap alerts={active}/></div><p className="muted map-help">Circles represent the radius stored with each alert. Production polygon/geofence data can be added when the authoritative source supplies it.</p></>}

          {view === 'create' && <CreateAlert form={form} setForm={setForm} onSubmit={createAlert} busy={busy}/>}

          {view === 'sources' && <DataSources/>}
          {view === 'users' && <UsersDevices/>}
          {view === 'notifications' && <Notifications/>}
          {['safety','audit'].includes(view) && <ComingSoon view={view}/>}

        </section>

      </div>

    </div>

  )

}



function Dashboard({summary,active,severe,tests,onAlerts,onCreate}) {

  return <>

    <div className="stat-grid">

      <div className="stat-card"><span className="stat-icon red">⚠</span><div><b>{summary?.activeAlerts ?? active.length}</b><small>Active alerts</small></div></div>

      <div className="stat-card"><span className="stat-icon orange">!</span><div><b>{severe}</b><small>Severe / extreme</small></div></div>

      <div className="stat-card"><span className="stat-icon blue">♙</span><div><b>{summary?.users ?? 0}</b><small>Registered users</small></div></div>

      <div className="stat-card"><span className="stat-icon green">◉</span><div><b>{summary?.notificationLogs ?? 0}</b><small>Notification records</small></div></div>

    </div>

    <div className="dashboard-grid">

      <div className="panel wide"><div className="panel-head"><div><span className="eyebrow">GEOGRAPHIC OVERVIEW</span><h2>Active alert map</h2></div><button className="secondary" onClick={onAlerts}>View alerts</button></div><AlertMap alerts={active}/></div>

      <div className="panel"><div className="panel-head"><div><span className="eyebrow">OPERATIONS</span><h2>Quick actions</h2></div></div><div className="quick-list"><button onClick={onCreate}>＋ Create development test alert</button><button onClick={onAlerts}>⚠ Review active alerts</button><button onClick={()=>alert('Production source configuration will be enabled after an authorized feed is configured.')}>⇄ Configure data source</button></div><div className="test-callout"><b>{tests}</b><span>active test alert(s)</span><small>Test alerts never represent official warnings.</small></div></div>

    </div>

  </>

}



function Alerts({alerts,onResolve,onCreate}) {

  const active=alerts.filter(a=>a.status==='ACTIVE')

  return <div className="panel"><div className="panel-head"><div><span className="eyebrow">ALERT MANAGEMENT</span><h2>Alert records</h2></div><button className="primary" onClick={onCreate}>＋ Create test alert</button></div>

    {active.length===0?<div className="empty-card compact"><h3>No active alerts</h3><p>There are no active alert records.</p></div>:<div className="table-wrap"><table><thead><tr><th>Alert</th><th>Type</th><th>Severity</th><th>Area</th><th>Source</th><th>Status</th><th></th></tr></thead><tbody>{active.map(a=><tr key={a.id}><td><b>#{a.id} {a.title}</b><small>{a.description}</small></td><td>{a.disasterType}</td><td><SeverityBadge severity={a.severity}/></td><td>{a.areaText||'—'}</td><td>{a.source}</td><td><span className="status active">{a.testAlert?'TEST':'ACTIVE'}</span></td><td><button className="danger-outline" onClick={()=>onResolve(a.id)}>Resolve</button></td></tr>)}</tbody></table></div>}

  </div>

}



function CreateAlert({form,setForm,onSubmit,busy}) {

  const set=(key,value)=>setForm({...form,[key]:value})

  return <form className="panel create-panel" onSubmit={onSubmit}>

    <div className="test-banner"><b>DEVELOPMENT / TEST MODE</b><span>This form creates a test record. It does not represent an official public emergency.</span></div>

    <div className="form-section"><span className="eyebrow">01 · ALERT DETAILS</span><div className="form-grid">

      <label>Alert title<input required value={form.title} onChange={e=>set('title',e.target.value)} placeholder="Example: Flash Flood Warning"/></label>

      <label>Disaster type<select value={form.disasterType} onChange={e=>set('disasterType',e.target.value)}>{['FLOOD','CYCLONE','EARTHQUAKE','TSUNAMI','LANDSLIDE','WILDFIRE','EXTREME_WEATHER','OTHER'].map(x=><option key={x}>{x}</option>)}</select></label>

      <label>Severity<select value={form.severity} onChange={e=>set('severity',e.target.value)}>{['MINOR','MODERATE','HIGH','SEVERE','EXTREME'].map(x=><option key={x}>{x}</option>)}</select></label>

      <label>Source<input value={form.source} onChange={e=>set('source',e.target.value)} placeholder="DEVELOPMENT_TEST"/></label>

      <label className="full-col">Description<textarea required value={form.description} onChange={e=>set('description',e.target.value)} placeholder="Describe the alert clearly."/></label>

      <label className="full-col">Safety instructions<textarea value={form.instructions} onChange={e=>set('instructions',e.target.value)} placeholder="Tell people what action to take."/></label>

    </div></div>

    <div className="form-section"><span className="eyebrow">02 · GEOGRAPHIC TARGET</span><div className="form-grid">

      <label>Latitude<input type="number" step="any" required value={form.centerLatitude} onChange={e=>set('centerLatitude',e.target.value)}/></label>

      <label>Longitude<input type="number" step="any" required value={form.centerLongitude} onChange={e=>set('centerLongitude',e.target.value)}/></label>

      <label>Radius (km)<input type="number" step="0.1" min="0.1" required value={form.radiusKm} onChange={e=>set('radiusKm',e.target.value)}/></label>

      <label>Affected area<input value={form.areaText} onChange={e=>set('areaText',e.target.value)} placeholder="District / town / zone"/></label>

      <label className="full-col">Source alert ID<input value={form.sourceAlertId} onChange={e=>set('sourceAlertId',e.target.value)} placeholder="Optional authoritative identifier"/></label>

    </div></div>

    <div className="form-actions"><button type="button" className="secondary" onClick={()=>location.hash='#/admin'}>Cancel</button><button className="primary" disabled={busy}>{busy?'Creating…':'Create TEST Alert'}</button></div>

  </form>

}





function DataSources() {

  const [sources, setSources] = useState([])

  const [busy, setBusy] = useState(null)

  const [message, setMessage] = useState('')

  const [error, setError] = useState('')



  const load = async () => {

    try {

      setError('')

      setSources(await api('/sources'))

    } catch (e) {

      setError(e.message)

    }

  }



  useEffect(() => { load() }, [])



  const sync = async id => {

    setBusy(id); setMessage(''); setError('')

    try {

      const source = await api(`/sources/${id}/sync`, { method: 'POST' })

      setMessage(`${source.name}: synchronization completed. ${source.lastError || ''}`)

      await load()

    } catch (e) {

      setError(e.message)

    } finally {

      setBusy(null)

    }

  }



  const toggle = async source => {

    setBusy(`toggle-${source.id}`); setMessage(''); setError('')

    try {

      await api(`/sources/${source.id}/enabled?value=${!source.enabled}`, { method: 'PATCH' })

      await load()

    } catch (e) {

      setError(e.message)

    } finally {

      setBusy(null)

    }

  }



  return <div className="source-page">

    {message && <div className="success-box">{message}</div>}

    {error && <div className="error-box">{error}</div>}



    <div className="source-intro panel">

      <div>

        <span className="eyebrow">AUTHORITATIVE INPUT</span>

        <h2>Government disaster data sources</h2>

        <p>

          The system consumes configured CAP/RSS sources on the server, validates the

          CAP payload, deduplicates alerts and stores normalized records for the dashboard.

        </p>

      </div>

      <div className="source-principle">

        <b>ETag aware</b>

        <span>Unchanged feeds return 304 and are not downloaded again.</span>

      </div>

    </div>



    <div className="source-grid">

      {sources.map(source => {

        const healthy = source.lastHttpStatus === 200 || source.lastHttpStatus === 304

        return <article className="source-card panel" key={source.id}>

          <div className="source-card-head">

            <div>

              <div className="source-title"><span className={`source-dot ${healthy ? 'healthy' : source.lastHttpStatus ? 'failed' : 'unknown'}`}></span>{source.name}</div>

              <div className="source-code">{source.code} · {source.type}</div>

            </div>

            <span className={`source-state ${source.enabled ? 'on' : 'off'}`}>{source.enabled ? 'ENABLED' : 'DISABLED'}</span>

          </div>



          <div className="source-url">{source.url}</div>



          <div className="source-metrics">

            <div><b>{source.lastItemCount ?? '—'}</b><span>RSS items</span></div>

            <div><b>{source.lastHttpStatus ?? '—'}</b><span>HTTP</span></div>

            <div><b>{source.lastSuccessAt ? new Date(source.lastSuccessAt).toLocaleString() : 'Never'}</b><span>Last success</span></div>

          </div>



          {source.lastError && <div className={source.lastError.startsWith('OK:') ? 'source-ok' : 'source-error'}>{source.lastError}</div>}



          <div className="source-actions">

            <button className="secondary" onClick={() => toggle(source)} disabled={busy !== null}>

              {source.enabled ? 'Disable' : 'Enable'}

            </button>

            <button className="primary" onClick={() => sync(source.id)} disabled={busy !== null || !source.enabled}>

              {busy === source.id ? 'Syncing…' : 'Sync now'}

            </button>

          </div>

        </article>

      })}

    </div>



    <div className="panel source-flow">

      <span className="eyebrow">INGESTION PIPELINE</span>

      <div className="flow">

        <span>Government CAP RSS</span><i>→</i><span>ETag check</span><i>→</i><span>CAP XML</span><i>→</i><span>Validate & normalize</span><i>→</i><span>Deduplicate</span><i>→</i><span>Disaster alerts DB</span>

      </div>

      <p className="muted">This server-side flow is the foundation for later mobile push/SMS delivery. It does not invent emergency alerts.</p>

    </div>

  </div>

}



function UsersDevices() {
  const [users, setUsers] = useState([])
  const [devices, setDevices] = useState([])
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    name:'', email:'', phone:'', password:'ChangeMe@123',
    latitude:'12.9716', longitude:'77.5946', district:'Bengaluru Urban', state:'Karnataka'
  })

  const load = async () => {
    try {
      setError('')
      const [u,d] = await Promise.all([api('/users'), api('/devices')])
      setUsers(u); setDevices(d)
    } catch(e) { setError(e.message) }
  }
  useEffect(()=>{ load() },[])

  const set=(k,v)=>setForm({...form,[k]:v})

  const create = async e => {
    e.preventDefault(); setBusy(true); setError(''); setMessage('')
    try {
      await api('/users',{method:'POST',body:JSON.stringify({
        ...form,
        latitude:Number(form.latitude),
        longitude:Number(form.longitude)
      })})
      setMessage('User registered with geographic location.')
      setForm({...form,name:'',email:'',phone:''})
      await load()
    } catch(e){setError(e.message)} finally{setBusy(false)}
  }

  return <div className="source-page">
    {message && <div className="success-box">{message}</div>}
    {error && <div className="error-box">{error}</div>}

    <div className="source-intro panel">
      <div>
        <span className="eyebrow">RECIPIENT REGISTRY</span>
        <h2>Users & Devices</h2>
        <p>Register recipients with a geographic location. Phase 6 uses that location to determine whether a government alert affects them.</p>
      </div>
      <div className="source-principle">
        <b>{users.filter(u=>u.enabled).length} enabled</b>
        <span>{devices.length} registered device(s)</span>
      </div>
    </div>

    <form className="panel create-panel" onSubmit={create}>
      <div className="form-section">
        <span className="eyebrow">ADD RECIPIENT</span>
        <div className="form-grid">
          <label>Name<input required value={form.name} onChange={e=>set('name',e.target.value)}/></label>
          <label>Email<input required type="email" value={form.email} onChange={e=>set('email',e.target.value)}/></label>
          <label>Phone<input value={form.phone} onChange={e=>set('phone',e.target.value)} placeholder="+91..."/></label>
          <label>Initial password<input type="password" value={form.password} onChange={e=>set('password',e.target.value)}/></label>
          <label>Latitude<input required type="number" step="any" value={form.latitude} onChange={e=>set('latitude',e.target.value)}/></label>
          <label>Longitude<input required type="number" step="any" value={form.longitude} onChange={e=>set('longitude',e.target.value)}/></label>
          <label>District<input value={form.district} onChange={e=>set('district',e.target.value)}/></label>
          <label>State<input value={form.state} onChange={e=>set('state',e.target.value)}/></label>
        </div>
      </div>
      <div className="form-actions"><button className="primary" disabled={busy}>{busy?'Registering…':'Register user'}</button></div>
    </form>

    <div className="panel">
      <div className="panel-head"><div><span className="eyebrow">REGISTERED RECIPIENTS</span><h2>Geographic recipients</h2></div><button className="secondary" onClick={load}>↻ Refresh</button></div>
      <div className="table-wrap"><table><thead><tr><th>User</th><th>Contact</th><th>Location</th><th>Devices</th><th>Status</th></tr></thead>
        <tbody>{users.map(u=>{
          const count=devices.filter(d=>d.userId===u.id).length
          return <tr key={u.id}><td><b>{u.name}</b><small>#{u.id} · {u.email}</small></td><td>{u.phone||'—'}</td><td>{u.latitude!=null&&u.longitude!=null?<><b>{Number(u.latitude).toFixed(4)}, {Number(u.longitude).toFixed(4)}</b><small>{u.district||''}{u.state?`, ${u.state}`:''}</small></>:'No location'}</td><td>{count}</td><td><span className={`status ${u.enabled?'active':'inactive'}`}>{u.enabled?'ENABLED':'DISABLED'}</span></td></tr>
        })}</tbody>
      </table></div>
    </div>
  </div>
}

function Notifications() {
  const [alerts,setAlerts]=useState([])
  const [selected,setSelected]=useState(null)
  const [logs,setLogs]=useState([])
  const [busy,setBusy]=useState(false)
  const [error,setError]=useState('')
  const [message,setMessage]=useState('')

  const load=async()=>{
    try{setError('');setAlerts(await api('/alerts'))}catch(e){setError(e.message)}
  }
  useEffect(()=>{load()},[])

  const prepare=async id=>{
    setBusy(true);setError('');setMessage('')
    try{
      const r=await api(`/notifications/alert/${id}/prepare`,{method:'POST'})
      setMessage(`Targeting completed: ${r.targetedUsers} user(s), ${r.targetedDevices} device(s). No external message was sent.`)
      setSelected(id)
      setLogs(await api(`/notifications/alert/${id}`))
    }catch(e){setError(e.message)}finally{setBusy(false)}
  }

  return <div className="source-page">
    {message&&<div className="success-box">{message}</div>}
    {error&&<div className="error-box">{error}</div>}
    <div className="source-intro panel">
      <div><span className="eyebrow">DELIVERY PREPARATION</span><h2>Notification targeting</h2><p>Preview which registered users/devices fall inside an alert circle. This phase does not claim FCM or SMS delivery.</p></div>
      <div className="source-principle"><b>Geographic</b><span>Haversine radius targeting</span></div>
    </div>
    <div className="panel">
      <div className="panel-head"><div><span className="eyebrow">ACTIVE & RECENT</span><h2>Alert targeting</h2></div><button className="secondary" onClick={load}>↻ Refresh</button></div>
      <div className="table-wrap"><table><thead><tr><th>Alert</th><th>Area</th><th>Severity</th><th>Action</th></tr></thead><tbody>
      {alerts.filter(a=>a.status==='ACTIVE').map(a=><tr key={a.id}><td><b>#{a.id} {a.title}</b><small>{a.source}{a.testAlert?' · TEST':''}</small></td><td>{a.areaText||'Geographic circle'}</td><td><SeverityBadge severity={a.severity}/></td><td><button className="primary" onClick={()=>prepare(a.id)} disabled={busy}>Target recipients</button></td></tr>)}
      </tbody></table></div>
    </div>
    {selected&&<div className="panel"><div className="panel-head"><div><span className="eyebrow">TARGETING LOG</span><h2>Alert #{selected}</h2></div></div><div className="table-wrap"><table><thead><tr><th>User</th><th>Channel</th><th>Status</th><th>Created</th></tr></thead><tbody>{logs.map(l=><tr key={l.id}><td>{l.userId}</td><td>{l.channel}</td><td>{l.status}</td><td>{l.createdAt?new Date(l.createdAt).toLocaleString():''}</td></tr>)}</tbody></table></div></div>}
  </div>
}

function ComingSoon({view}) {

  const names={notifications:'Notifications & Delivery Logs',users:'Users & Devices',sources:'Authorized Data Sources',safety:'Emergency Services & Shelters',audit:'Audit Logs'}

  return <div className="panel placeholder"><div className="placeholder-icon">◌</div><span className="eyebrow">NEXT MODULE</span><h2>{names[view]}</h2><p>The backend foundation is already present. This module will be connected to its real provider/data source in the next implementation phase.</p></div>

}



function getRoute() {
  if (location.hash) return location.hash
  if (location.pathname === '/admin' || location.pathname.startsWith('/admin/')) return '#/admin'
  return '#/'
}

function App() {

  const [path,setPath]=useState(getRoute)

  useEffect(()=>{
    const fn=()=>setPath(getRoute())
    addEventListener('hashchange',fn)
    addEventListener('popstate',fn)
    return()=>{
      removeEventListener('hashchange',fn)
      removeEventListener('popstate',fn)
    }
  },[])

  return path.startsWith('#/admin') ? <Admin/> : <Public/>

}

createRoot(document.getElementById('root')).render(<App/>)
