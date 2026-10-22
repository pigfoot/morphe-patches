import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source = fs.readFileSync(new URL('../patches/src/main/kotlin/app/pigfoot/patches/camscanner/CamScannerQuotaProbe.kt', import.meta.url), 'utf8');
const raw = source.split('internal val quotaProbeScript = """')[1].split('""".trimIndent()')[0];
const script = raw.replace('function(url) { return import(url); }', '__importer');
assert.notEqual(raw, script);
const canary = 'PRIVATE_ACCOUNT_TOKEN_NOT_FOR_DISPLAY';
const legacyUrl = 'https://static-cdn.camscanner.com/camscanner-mo/static/pc-api-BrpmLW-U.js';
const modernUrl = 'https://static-cdn.camscanner.com/camscanner-integral-mo/static/config-Dyz9RPJ4.js';
const legacyHash = 'be6d3ff01592d1e952392801b2f5536cda89486f451cc90d1d060c219c23b636';
const modernHash = '29cb6e0058ef876d76b6dd60f611d54cc8cb62ab6a6a637578c1d34b5978c646';

function fixture(options = {}) {
  const modern = !!options.modern, nodes = new Map(), timers = new Map();
  let timerId = 0, queryCalls = 0, imports = 0, publicFetches = 0, mutation;
  const loaded = modern ? modernUrl : legacyUrl;
  const location = new URL(options.location || `https://mo.camscanner.com/${modern ? 'integral/intergralTurntable' : 'lottery/relottery'}?intsig_key=${canary}&encrypt_uid=${canary}&client_app=CamScanner&version=new`);
  class Xhr {
    constructor() { this.listeners = []; this.status = options.http ?? 200; this.responseType = ''; this.responseText = JSON.stringify(options.rawBody || {ret:'200',data:{[modern ? 'day_count' : 'watchad_vip_chance']:3, token:canary}}); }
    open(method, url) { this.actualOpen = [method, url]; return 73; }
    addEventListener(name, fn) { assert.equal(name,'loadend'); this.listeners.push(fn); }
    getResponseHeader(name) { assert.equal(name,'X-IS-Error-Code'); return options.xerr ?? null; }
    finish() { for (const fn of this.listeners) fn.call(this); }
  }
  const originalOpen = Xhr.prototype.open;
  const quota = async (args) => {
    queryCalls++;
    assert.equal(queryCalls,1,'no retry or second quota read');
    if (modern) { assert.equal(args.token,canary); assert.equal(args.lottery_for_new,1); }
    else assert.equal(args.intsig_key,canary);
    const xhr = new Xhr();
    assert.equal(xhr.open('GET', `https://api-cs.intsig.net/user/cs/${modern ? 'reward/big_lottery?probe=1' : 'query_property?property_id=watchad_vip'}&token=${canary}`),73,'original XHR return is preserved');
    if (!options.pending) xhr.finish();
    if (options.multiple) { const other = new Xhr(); other.open('GET','https://api-cs.intsig.net/query_property?property_id=watchad_vip'); other.finish(); }
    if (options.error) throw new Error(canary + ' https://private.invalid/?token=' + canary);
    if (options.pending) return new Promise(()=>{});
    return options.result ?? {ret:'200',data:{[modern ? 'day_count' : 'watchad_vip_chance']:3}};
  };
  const context = {
    location, URL, URLSearchParams, Uint8Array, Reflect, Date, Number, Object, String,
    XMLHttpRequest:Xhr,
    performance:{getEntriesByType(type) { assert.equal(type,'resource'); return options.moduleAbsent ? [] : [{name:options.moduleUnknown ? loaded.replace(modern ? 'Dyz9RPJ4' : 'BrpmLW-U','ABCDEFGH') : loaded}]; }},
    setTimeout(fn,delay) { const id = ++timerId; timers.set(id,{fn,delay}); return id; },
    clearTimeout(id) { timers.delete(id); },
    MutationObserver:class { constructor(fn) { mutation=fn; } observe() {} disconnect() {} },
    document:{documentElement:{appendChild(node) { nodes.set(node.id,node); }}, getElementById(id) { return nodes.get(id); }, createElement() { return {style:{}}; }, querySelector() { return options.wheel ? {} : null; }},
    fetch:async (url,config) => { publicFetches++; assert.equal(url,loaded); assert.equal(config.credentials,'omit'); return {ok:options.fetchOk!==false,arrayBuffer:async()=>new Uint8Array([1,2,3]).buffer}; },
    __importer:async url => { imports++; assert.equal(url,loaded); if(options.importError) throw Error(canary); return options.functionMissing ? {} : modern ? {I:quota,c:{macro:{token:options.noToken?'':canary}}} : {queryLottery:quota}; },
  };
  context.window={crypto:{subtle:{async digest(algorithm,bytes) {
    assert.equal(algorithm,'SHA-256'); assert.deepEqual([...new Uint8Array(bytes)],[1,2,3]);
    // Model the browser's digest boundary; the real public-source hashes were independently measured.
    return Uint8Array.from(Buffer.from(options.hashMismatch ? '00'.repeat(32) : modern ? modernHash : legacyHash,'hex')).buffer;
  }}}};
  context.crypto=context.window.crypto;
  vm.createContext(context); vm.runInContext(script,context);
  async function flush() {
    for(let i=0;i<80;i++) {
      await Promise.resolve();
      const entry=[...timers].find(([,t])=>t.delay===100);
      if(entry) { timers.delete(entry[0]); entry[1].fn(); }
    }
  }
  function report() { return nodes.get('cs-quota-probe-v16')?.textContent || ''; }
  return {context,flush,report,get queryCalls(){return queryCalls;},get imports(){return imports;},get publicFetches(){return publicFetches;},originalOpen,
    repeat(){vm.runInContext(script,context);mutation?.();},
    timeout(){const entry=[...timers].find(([,t])=>t.delay===10000);assert.ok(entry);timers.delete(entry[0]);entry[1].fn();},
    assertPrivate(){assert.ok(!report().includes(canary));assert.ok(!report().includes('https://'));},
  };
}

test('legacy zero, three, null, missing and false remain distinct without drawing',async()=>{
  for(const [result,wanted] of [[{data:{watchad_vip_chance:0}},'COUNT_0'],[{data:{watchad_vip_chance:3}},'COUNT_3'],[{data:{watchad_vip_chance:null}},'NULL'],[{data:{}},'MISSING'],[{data:false},'QUERY_FALSE'],[{data:{watchad_vip_chance:canary}},'INVALID_TYPE']]) {
    const f=fixture({result});await f.flush();assert.match(f.report(),new RegExp(`QUERY=${wanted}\\n`));assert.equal(f.queryCalls,1);f.assertPrivate();assert.equal(f.context.XMLHttpRequest.prototype.open,f.originalOpen);
  }
});
test('modern original read uses day_count and preserves actual business ret',async()=>{
  const f=fixture({modern:true,result:{ret:'406',data:{day_count:0}},http:406,xerr:'103'});await f.flush();assert.match(f.report(),/ROUTE=MODERN_DAY_COUNT/);assert.match(f.report(),/QUERY=COUNT_0/);assert.match(f.report(),/HTTP_OBS=406/);assert.match(f.report(),/QUERY_RET=406/);f.assertPrivate();
});
test('duplicate page finish never reads twice',async()=>{const f=fixture();f.repeat();await f.flush();f.repeat();await f.flush();assert.equal(f.queryCalls,1);assert.equal(f.imports,1);});
test('module absence, hash failure, fetch failure, import failure and missing function fail without quota read',async()=>{
  for(const [opts,state] of [[{moduleAbsent:true},'MODULE_NOT_LOADED'],[{hashMismatch:true},'MODULE_HASH_REJECTED'],[{fetchOk:false},'MODULE_FETCH_FAILED'],[{importError:true},'MODULE_ERROR_NO_READ'],[{functionMissing:true},'FUNCTION_MISSING']]) {
    const f=fixture(opts);await f.flush();assert.match(f.report(),new RegExp(`QUERY=${state}`));assert.equal(f.queryCalls,0);f.assertPrivate();
  }
});
test('missing modern token cancels and restores observer',async()=>{const f=fixture({modern:true,noToken:true});await f.flush();assert.match(f.report(),/QUERY=NO_TOKEN/);assert.equal(f.queryCalls,0);assert.equal(f.context.XMLHttpRequest.prototype.open,f.originalOpen);});
test('wrong host and unknown lottery route never import or read',async()=>{
  for(const location of ['https://mo.camscanner.com.evil.invalid/lottery/relottery','https://mo.camscanner.com/lottery/unknown']) {const f=fixture({location});await f.flush();assert.equal(f.queryCalls,0);assert.equal(f.imports,0);assert.equal(f.publicFetches,0);f.assertPrivate();}
});
test('HTTP/business/network failure does not print error messages or credentials',async()=>{
  const f=fixture({error:true,http:406,xerr:'103',rawBody:{ret:'103',data:{token:canary}}});await f.flush();assert.match(f.report(),/QUERY=QUERY_ERROR_NO_DETAILS/);assert.match(f.report(),/HTTP_OBS=406/);assert.match(f.report(),/XERR_OBS=103/);f.assertPrivate();assert.equal(f.context.XMLHttpRequest.prototype.open,f.originalOpen);
});
test('timeout does not retry or abort the original request',async()=>{const f=fixture({pending:true});await f.flush();f.timeout();await f.flush();assert.equal(f.queryCalls,1);assert.match(f.report(),/QUERY=TIMEOUT_NO_RETRY/);assert.equal(f.context.XMLHttpRequest.prototype.open,f.originalOpen);f.assertPrivate();});
test('concurrent observed reads are labeled rather than claimed to be correlated',async()=>{const f=fixture({multiple:true});await f.flush();assert.match(f.report(),/OBSERVED_READS=2/);assert.match(f.report(),/QUOTA_READS=1/);assert.equal(f.queryCalls,1);f.assertPrivate();});

test('unqualified public module build tag is reported without import or quota read',async()=>{const f=fixture({moduleUnknown:true});await f.flush();assert.match(f.report(),/MODULE_TAG=pc-api-ABCDEFGH.js/);assert.match(f.report(),/QUERY=MODULE_NOT_LOADED/);assert.equal(f.queryCalls,0);assert.equal(f.imports,0);assert.equal(f.publicFetches,0);f.assertPrivate();});
