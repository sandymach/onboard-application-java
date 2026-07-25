package com.ikanobank.onboarding.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WebOnboardingController {
    @GetMapping("/")
    public String root() {
        return "redirect:/onboarding";
    }

    @GetMapping(value = "/onboarding", produces = "text/html")
    @ResponseBody
    public String onboarding() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>Ikano account application</title>
                  <style>
                    body{font-family:Arial,sans-serif;margin:0;background:#f6f7f9;color:#1f2937}
                    main{max-width:1120px;margin:0 auto;padding:2rem}
                    h1,h2,h3{margin:.25rem 0 .75rem}
                    label{display:block;margin:.75rem 0 .25rem;font-weight:600}
                    select,input,button{padding:.65rem;border:1px solid #cbd5e1;border-radius:6px}
                    input,select{min-width:240px}
                    button{cursor:pointer;background:#174ea6;color:white;border-color:#174ea6;margin:.25rem}
                    button.secondary{background:white;color:#174ea6}
                    button.danger{background:#b42318;border-color:#b42318}
                    button:disabled{opacity:.45;cursor:not-allowed}
                    .grid{display:grid;grid-template-columns:1.2fr .8fr;gap:1rem}
                    .row{display:flex;gap:1rem;flex-wrap:wrap;align-items:end}
                    .card{background:white;border:1px solid #e5e7eb;border-radius:10px;padding:1rem;margin:1rem 0;box-shadow:0 1px 2px #0001}
                    .note{background:#eef6ff;border-left:4px solid #174ea6;padding:.75rem;margin:.75rem 0}
                    .warn{background:#fff7ed;border-left-color:#f97316}
                    .err{background:#fef2f2;border-left-color:#dc2626}
                    .ok{background:#ecfdf3;border-left-color:#16a34a}
                    .brand{font-weight:800;letter-spacing:.02em;color:#ffd200}
                    .hero{background:#003b71;color:white;padding:1.5rem;border-radius:12px;margin-bottom:1rem}
                    .hero p{color:#e5edf9}
                    .screen{display:none}.screen.active{display:block}
                    .steps{display:flex;gap:.5rem;flex-wrap:wrap;margin:.75rem 0}
                    .pill{padding:.35rem .55rem;border-radius:999px;background:#e5e7eb;font-size:.85rem}
                    .pill.active{background:#174ea6;color:white}
                    .pill.done{background:#dcfce7;color:#166534}
                    pre{background:#111827;color:#e5e7eb;padding:1rem;border-radius:8px;overflow:auto;max-height:420px}
                    @media(max-width:850px){.grid{grid-template-columns:1fr}}
                  </style>
                </head>
                <body>
                <main>
                  <div class="hero">
                    <div class="brand">IKANO BANK ONBOARDING</div>
                    <h1>Apply for an Ikano account</h1>
                    <p>Tell us who you are, confirm your details and submit your application. Ikano Bank adapts the journey by country and account type.</p>
                  </div>

                  <section id="setupScreen" class="screen active">
                    <div class="card">
                      <h2>Demo setup</h2>
                      <p>Select product, country and applicant type first. The mock scenario list below is filtered to show only scenarios available for that selection.</p>
                      <div class="row">
                        <div><label>Product code</label><select id="productCode" onchange="refreshScenarioOptions()"><option value="IKANO_ONBOARDING_LOAN">IKANO_ONBOARDING_LOAN</option><option value="IKANO_BUSINESS_ACCOUNT">IKANO_BUSINESS_ACCOUNT</option></select></div>
                        <div><label>Where do you live / operate?</label><select id="country" onchange="refreshScenarioOptions()"><option>SWEDEN</option><option>SPAIN</option><option>POLAND</option></select></div>
                        <div><label>Who is applying?</label><select id="customerType" onchange="syncProductAndRefresh()"><option value="PRIVATE_INDIVIDUAL">Private individual</option><option value="BUSINESS">Business</option></select></div>
                        <div>
                          <label>Available mock scenario</label>
                          <select id="preset" onchange="applyPreset()"></select>
                        </div>
                        <div><label>Selected mock key</label><input id="scenarioKey" value="default" readonly/></div>
                      </div>
                      <div id="scenarioNote" class="note"></div>
                      <div id="scenarioDetails" class="note"></div>
                      <button id="startBtn" onclick="guard(createApplication)">Continue to Ikano application</button>
                    </div>
                  </section>

                  <div id="journeyScreen" class="screen">
                  <div class="grid">
                    <section>
                      <div class="card">
                        <h2>1. Choose your application</h2>
                        <p id="selectedJourney">Your Ikano onboarding journey is being prepared.</p>
                        <button class="secondary" onclick="backToScenarios()">Back to scenarios</button>
                        <button class="secondary" onclick="startNewApplication()">Start new application</button>
                      </div>

                      <div class="card">
                        <h2>2. Fill in your details</h2>
                        <div id="progress" class="note">Start an application first.</div>
                        <div id="stepPills" class="steps"></div>
                        <div id="stepForm"></div>
                        <button id="stepBtn" onclick="guard(submitCurrentStep)" disabled>Continue</button>
                      </div>

                      <div class="card">
                        <h2>3. Submit and complete</h2>
                        <div id="actionNote" class="note">The recommended next action will appear here.</div>
                        <button id="submitBtn" onclick="guard(customerSubmit)" disabled>Submit application</button>
                        <button id="agreementBtn" onclick="guard(createAgreement)" disabled>Continue to agreement</button>
                        <button id="signBtn" onclick="guard(signAgreement)" disabled>Sign agreement</button>
                        <button id="signLaterBtn" class="secondary" onclick="guard(signLater)" disabled>Sign later</button>
                        <button id="accountBtn" onclick="guard(setupAccount)" disabled>Finish setup</button>
                        <div id="documentLinks" class="note" style="display:none"></div>
                        <div id="agreementDetails" class="card" style="display:none"></div>
                      </div>
                    </section>

                    <aside>
                      <div class="card">
                        <h2>Application status</h2>
                        <div id="stateSummary">No application yet.</div>
                        <button class="secondary" onclick="guard(loadAudit)" id="auditBtn" disabled>Refresh audit</button>
                      </div>
                      <div class="card">
                        <h2>Technical response / audit</h2>
                        <pre id="output">{}</pre>
                      </div>
                      <div class="card">
                        <h2>Operations panel</h2>
                        <p>Shown for interview/demo only. Customers do not manually approve or decline their own referred applications.</p>
                        <button id="manualApproveBtn" class="secondary" onclick="guard(manualApprove)" disabled>Approve referred application</button>
                        <button id="manualDeclineBtn" class="danger" onclick="guard(manualDecline)" disabled>Decline referred application</button>
                      </div>
                      <div class="card">
                        <h2>Error scenario demos</h2>
                        <p>Use these to show backend validation and standard API error responses without leaving the web app.</p>
                        <button class="secondary" onclick="guard(demoBadRequest)">400 validation failure</button>
                        <button class="secondary" onclick="guard(demoNotFound)">404 unknown application</button>
                        <button class="secondary" onclick="guard(demoConflict)">409 wrong lifecycle state</button>
                        <p class="note">403 and 500 are documented in OpenAPI. This demo has no auth layer, and intentionally crashing the app to force 500 is avoided.</p>
                      </div>
                    </aside>
                  </div>
                  </div>
                </main>
                <script>
                  let app=null, flow=null, stepIndex=0, checksDone=false, submitted=false, agreementGenerated=false, signed=false, signConfirmed=false, lastChecks=[];
                  function requestHeaders(){return {'Content-Type':'application/json','X-Channel':'WEB','X-Product-Code':productCode.value}}
                  const presets=[
                    {name:'Approved - Sweden private loan',product:'IKANO_ONBOARDING_LOAN',country:'SWEDEN',type:'PRIVATE_INDIVIDUAL',key:'default',outcome:'Approved after policy checks',note:'Underwriting, AML and fraud/KYC policies pass. Final customer path reaches agreement, signing and APPROVED.',checks:['Fraud/KYC: identity verified', 'AML: no sanctions or PEP hit', 'Underwriting: affordability passed', 'Address verification: address matched']},
                    {name:'Manual review - Sweden private identity',product:'IKANO_ONBOARDING_LOAN',country:'SWEDEN',type:'PRIVATE_INDIVIDUAL',key:'2222',outcome:'Referred to manual review',note:'Fraud/KYC policy note: identity confidence is medium and additional document review is required.',checks:['Fraud/KYC: ADDITIONAL_DOCUMENT_REQUIRED', 'AML: not blocking', 'Underwriting: not blocking'],operations:'Use Operations: approve referred application to continue to agreement, or decline to end the journey.'},
                    {name:'Declined - Sweden private identity',product:'IKANO_ONBOARDING_LOAN',country:'SWEDEN',type:'PRIVATE_INDIVIDUAL',key:'3333',outcome:'Declined',note:'Fraud/KYC policy decline: identity document mismatch and low verification confidence.',checks:['Fraud/KYC: DOCUMENT_MISMATCH', 'Policy result: terminal decline']},
                    {name:'Approved - Spain private loan',product:'IKANO_ONBOARDING_LOAN',country:'SPAIN',type:'PRIVATE_INDIVIDUAL',key:'default',outcome:'Approved after policy checks',note:'All Spain private checks pass and the applicant continues to agreement/signing.',checks:['Fraud/KYC: DNI/NIE verified', 'AML: no sanctions hit', 'Underwriting: affordability passed']},
                    {name:'Manual review - Spain private AML',product:'IKANO_ONBOARDING_LOAN',country:'SPAIN',type:'PRIVATE_INDIVIDUAL',key:'4444',outcome:'Referred to manual review',note:'AML policy note: possible sanctions hit. Operations must review before approval/decline.',checks:['AML: POSSIBLE_HIT', 'Fraud/KYC: identity passed', 'Underwriting: not blocking'],operations:'Good scenario to explain AML escalation and manual review controls.'},
                    {name:'Declined - Spain private underwriting',product:'IKANO_ONBOARDING_LOAN',country:'SPAIN',type:'PRIVATE_INDIVIDUAL',key:'7777',outcome:'Declined',note:'Underwriting policy decline: affordability requirements are not met.',checks:['Underwriting: UNAFFORDABLE', 'Policy result: terminal decline']},
                    {name:'Approved - Poland private loan',product:'IKANO_ONBOARDING_LOAN',country:'POLAND',type:'PRIVATE_INDIVIDUAL',key:'default',outcome:'Approved after policy checks',note:'All Poland private checks pass and the applicant continues to agreement/signing.',checks:['Fraud/KYC: PESEL verified', 'AML: no sanctions hit', 'Underwriting: affordability passed']},
                    {name:'Declined - Poland private AML',product:'IKANO_ONBOARDING_LOAN',country:'POLAND',type:'PRIVATE_INDIVIDUAL',key:'5555',outcome:'Declined',note:'AML policy decline: confirmed sanctions match.',checks:['AML: CONFIRMED_HIT', 'Policy result: terminal decline']},
                    {name:'Approved - Sweden business account',product:'IKANO_BUSINESS_ACCOUNT',country:'SWEDEN',type:'BUSINESS',key:'default',outcome:'Approved after KYB checks',note:'Company, representative, registry and account checks pass.',checks:['KYB: active company', 'Fraud/KYC: representative verified', 'Bank account: verified']},
                    {name:'Manual review - Sweden business registry',product:'IKANO_BUSINESS_ACCOUNT',country:'SWEDEN',type:'BUSINESS',key:'8888',outcome:'Referred to manual review',note:'KYB/fraud policy note: representative authority could not be confirmed.',checks:['KYB: UNKNOWN_REPRESENTATIVE', 'Registry: company requires manual authority review'],operations:'Use this to demonstrate business onboarding and representative checks.'},
                    {name:'Approved - Spain business account',product:'IKANO_BUSINESS_ACCOUNT',country:'SPAIN',type:'BUSINESS',key:'default',outcome:'Approved after KYB checks',note:'Spain business KYB and registry checks pass.',checks:['KYB: company active', 'Representative authority: confirmed']},
                    {name:'Declined - Spain business registry',product:'IKANO_BUSINESS_ACCOUNT',country:'SPAIN',type:'BUSINESS',key:'9999',outcome:'Declined',note:'KYB policy decline: registry indicates dissolved company.',checks:['KYB: DISSOLVED_COMPANY', 'Policy result: terminal decline']},
                    {name:'Approved - Poland business account',product:'IKANO_BUSINESS_ACCOUNT',country:'POLAND',type:'BUSINESS',key:'default',outcome:'Approved after KYB checks',note:'Poland business KYB and bank account checks pass.',checks:['KYB: company active', 'Bank account: IBAN verified']},
                    {name:'Manual review - Poland business bank account',product:'IKANO_BUSINESS_ACCOUNT',country:'POLAND',type:'BUSINESS',key:'1212',outcome:'Referred to manual review',note:'Fraud/payment policy note: account-holder name mismatch requires review.',checks:['Fraud/payment: NAME_MISMATCH', 'Bank account ownership requires manual review'],operations:'Good scenario to explain payment fraud controls.'}
                  ];
                  const samples={
                    personalNumber:'19940608-1111', dniNie:'X1234567L', pesel:'94060812345',
                    email:'customer.demo@example.com', phone:'+46701234567', address:'Demo Street 12, Stockholm',
                    consentAccepted:'true', pepDeclaration:'false', taxResidency:'SE',
                    employmentStatus:'PERMANENT', monthlyIncome:'42000', monthlyDebt:'2500', termsAccepted:'true',
                    organisationNumber:'556677-8899', companyNif:'B12345678', companyIdentifier:'PL1234567890',
                    legalName:'Demo Trading AB', legalForm:'LIMITED_COMPANY',
                    representativeName:'Alex Demo', representativeIdentifier:'19800101-1234', authorityConfirmed:'true',
                    beneficialOwners:'Alex Demo 60%; Sam Demo 40%', businessActivity:'Retail finance demo',
                    annualTurnover:'2500000', expectedUsage:'Customer onboarding demo', creditConsent:'true'
                  };
                  function init(){
                    refreshScenarioOptions();
                  }
                  function availablePresets(){
                    return presets.filter(p=>p.product===productCode.value && p.country===country.value && p.type===customerType.value);
                  }
                  function syncProductAndRefresh(){
                    productCode.value=customerType.value==='BUSINESS'?'IKANO_BUSINESS_ACCOUNT':'IKANO_ONBOARDING_LOAN';
                    refreshScenarioOptions();
                  }
                  function refreshScenarioOptions(){
                    const available=availablePresets();
                    preset.innerHTML=available.map((p,i)=>`<option value="${i}">${p.name}</option>`).join('');
                    applyPreset();
                  }
                  function applyPreset(){
                    const available=availablePresets();
                    const p=available[preset.value||0] || available[0];
                    if(!p){scenarioNote.textContent='No mock scenario available for this selection.'; scenarioDetails.innerHTML=''; scenarioKey.value='default'; return;}
                    scenarioKey.value=p.key;
                    scenarioNote.textContent=p.note;
                    scenarioDetails.innerHTML=`<b>Product:</b> ${p.product}<br><b>Country/applicant:</b> ${p.country} / ${p.type}<br><b>Expected outcome:</b> ${p.outcome}<br><b>Mock key:</b> ${p.key}<br><b>Policy/checks:</b><ul>${p.checks.map(c=>`<li>${c}</li>`).join('')}</ul>${p.operations?`<b>Demo note:</b> ${p.operations}`:''}`;
                  }
                  async function api(path, opts={}) {
                    const res = await fetch(path, {...opts, headers:{...requestHeaders(),...(opts.headers||{})}});
                    const txt = await res.text();
                    const body = txt ? JSON.parse(txt) : null;
                    if(!res.ok) throw new Error(body?.message || txt || res.statusText);
                    return body;
                  }
                  async function guard(fn){try{await fn()}catch(e){show({error:e.message}); actionNote.className='note err'; actionNote.textContent=e.message}}
                  function show(x){output.textContent=JSON.stringify(x,null,2)}
                  function sampleValue(field){return samples[field] || 'demo-' + field}
                  function resetUiState(){
                    app=null; flow=null; stepIndex=0; checksDone=false; submitted=false; agreementGenerated=false; signed=false; signConfirmed=false; lastChecks=[];
                    progress.textContent='Start an application first.';
                    stepPills.innerHTML=''; stepForm.innerHTML=''; output.textContent='{}';
                    stateSummary.textContent='No application yet.';
                    documentLinks.style.display='none'; agreementDetails.style.display='none';
                    actionNote.className='note'; actionNote.textContent='The recommended next action will appear here.';
                    updateButtons();
                  }
                  function backToScenarios(){
                    journeyScreen.classList.remove('active'); setupScreen.classList.add('active');
                  }
                  function startNewApplication(){
                    resetUiState();
                    journeyScreen.classList.remove('active'); setupScreen.classList.add('active');
                  }
                  async function createApplication(){
                    const body={country:country.value,customerType:customerType.value,scenarioKey:scenarioKey.value};
                    const created=await api('/api/v1/applications',{method:'POST',body:JSON.stringify(body)});
                    app=created.application; checksDone=false; submitted=false; agreementGenerated=false; signed=false; signConfirmed=false; lastChecks=[];
                    flow=await api(`/api/v1/flows/${app.country}/${app.customerType}`);
                    setupScreen.classList.remove('active'); journeyScreen.classList.add('active');
                    selectedJourney.innerHTML=`<b>Product:</b> ${productCode.value}<br><b>Selected Ikano journey:</b> ${app.country} / ${app.customerType.replace('_',' ').toLowerCase()}<br><b>Mock scenario:</b> ${preset.options[preset.selectedIndex]?.text || scenarioKey.value}<br><b>Scenario note:</b> ${scenarioNote.textContent}`;
                    stepIndex=0; await renderStep(); show(created); updateButtons();
                  }
                  async function renderStep(){
                    if(!app||!flow)return;
                    const steps=flow.steps.sort((a,b)=>a.order-b.order);
                    const step=steps[Math.min(stepIndex, steps.length-1)];
                    const fields=await api(`/api/v1/flows/${app.country}/${app.customerType}/steps/${step.code}/required-fields`);
                    progress.innerHTML=`Application <b>${app.id}</b><br>Status <b>${app.status}</b><br>Current step <b>${step.code}</b>: ${step.title||step.code}`;
                    stepPills.innerHTML=steps.map((s,i)=>`<span class="pill ${i<stepIndex?'done':i===stepIndex?'active':''}">${i+1}. ${s.code}</span>`).join('');
                    stepForm.innerHTML=(fields.length?fields:['value']).map(f=>`<label>${f}</label><input data-field="${f}" value="${sampleValue(f)}"/>`).join('');
                    updateButtons();
                  }
                  async function submitCurrentStep(){
                    const steps=flow.steps.sort((a,b)=>a.order-b.order);
                    const step=steps[stepIndex];
                    const answers={};
                    document.querySelectorAll('#stepForm input').forEach(i=>answers[i.dataset.field]=i.value);
                    app=await api(`/api/v1/applications/${app.id}/steps/${step.code}`,{method:'PUT',body:JSON.stringify({answers})});
                    if(stepIndex < steps.length-1) stepIndex++;
                    await renderStep(); show(app); updateButtons();
                  }
                  async function runChecks(){lastChecks=await api(`/api/v1/applications/${app.id}/checks`,{method:'POST'}); show(lastChecks); checksDone=true; updateButtons()}
                  async function submitApplication(){app=await api(`/api/v1/applications/${app.id}/submit`,{method:'POST'}); submitted=true; show({application:app, policyNotes:policyNotes()}); updateButtons()}
                  async function customerSubmit(){await runChecks(); await submitApplication()}
                  async function createAgreement(){show(await api(`/api/v1/applications/${app.id}/agreement`,{method:'POST'})); agreementGenerated=true; signConfirmed=false; app=await api(`/api/v1/applications/${app.id}`); updateButtons()}
                  async function signAgreement(){if(!signConfirmed) throw new Error('Please confirm the click-to-sign checkbox before signing.'); show(await api(`/api/v1/applications/${app.id}/agreement/sign`,{method:'POST'})); signed=true; app=await api(`/api/v1/applications/${app.id}`); updateButtons()}
                  async function signLater(){
                    const result=await api(`/api/v1/applications/${app.id}/agreement/sign-later`,{method:'POST'});
                    show(result);
                    agreementGenerated=true;
                    app=await api(`/api/v1/applications/${app.id}`);
                    actionNote.className='note warn';
                    actionNote.textContent='Sign later selected. Return to the application later and choose Resume signing.';
                    updateButtons();
                  }
                  async function setupAccount(){show(await api(`/api/v1/applications/${app.id}/account-setup`,{method:'POST'})); app=await api(`/api/v1/applications/${app.id}`); updateButtons()}
                  async function manualApprove(){app=await api(`/api/v1/applications/${app.id}/manual-override`,{method:'POST',body:JSON.stringify({status:'AGREEMENT_CREATED',reason:'manual approval from web demo'})}); show(app); updateButtons()}
                  async function manualDecline(){app=await api(`/api/v1/applications/${app.id}/manual-override`,{method:'POST',body:JSON.stringify({status:'DECLINED',reason:'manual decline from web demo'})}); show(app); updateButtons()}
                  async function loadAudit(){show(await api(`/api/v1/applications/${app.id}/audit-events`))}
                  async function ensureDemoApplication(){
                    if(app) return app;
                    const created=await api('/api/v1/applications',{method:'POST',body:JSON.stringify({country:country.value,customerType:customerType.value,scenarioKey:scenarioKey.value})});
                    app=created.application;
                    flow=await api(`/api/v1/flows/${app.country}/${app.customerType}`);
                    return app;
                  }
                  async function demoBadRequest(){
                    const a=await ensureDemoApplication();
                    const badAnswers=a.customerType==='BUSINESS'?{organisationNumber:'bad-id',legalName:'X',legalForm:'LIMITED_COMPANY'}:{personalNumber:'bad-id'};
                    try{
                      await api(`/api/v1/applications/${a.id}/steps/${a.currentStepCode}`,{method:'PUT',body:JSON.stringify({answers:badAnswers})});
                    }catch(e){
                      actionNote.className='note err'; actionNote.textContent='Expected 400 validation failure: '+e.message; show({expectedStatus:400,error:e.message}); return;
                    }
                    throw new Error('Expected 400 but request passed');
                  }
                  async function demoNotFound(){
                    try{
                      await api('/api/v1/applications/00000000-0000-0000-0000-000000000000');
                    }catch(e){
                      actionNote.className='note err'; actionNote.textContent='Expected 404 not found: '+e.message; show({expectedStatus:404,error:e.message}); return;
                    }
                    throw new Error('Expected 404 but request passed');
                  }
                  async function demoConflict(){
                    const a=await ensureDemoApplication();
                    try{
                      await api(`/api/v1/applications/${a.id}/agreement`,{method:'POST'});
                    }catch(e){
                      actionNote.className='note err'; actionNote.textContent='Expected 409 lifecycle conflict: '+e.message; show({expectedStatus:409,error:e.message}); return;
                    }
                    throw new Error('Expected 409 but request passed');
                  }
                  function setEnabled(id,on){document.getElementById(id).disabled=!on}
                  function updateButtons(){
                    const has=!!app, ready=has&&app.status==='READY_FOR_REVIEW';
                    const status=has?app.status:'NONE';
                    setEnabled('stepBtn',has && ['INITIATED','IN_PROGRESS','KYC','IDV'].includes(status));
                    setEnabled('submitBtn',ready);
                    setEnabled('agreementBtn',(status==='AGREEMENT_CREATED'||status==='SIGNING_PENDING') && !agreementGenerated);
                    setEnabled('signBtn',(status==='AGREEMENT_CREATED'||status==='SIGNING_PENDING') && agreementGenerated && signConfirmed && !signed);
                    setEnabled('signLaterBtn',status==='AGREEMENT_CREATED' && agreementGenerated && !signed);
                    setEnabled('accountBtn',status==='AGREEMENT_SIGNED');
                    setEnabled('manualApproveBtn',status==='MANUAL_REVIEW');
                    setEnabled('manualDeclineBtn',status==='MANUAL_REVIEW');
                    setEnabled('auditBtn',has);
                    stateSummary.innerHTML=has?`<p><b>ID:</b> ${app.id}</p><p><b>Status:</b> ${status}</p><p><b>Country:</b> ${app.country}</p><p><b>Type:</b> ${app.customerType}</p>${policySummaryHtml()}`:'No application yet.';
                    documentLinks.style.display=agreementGenerated?'block':'none';
                    documentLinks.innerHTML=agreementGenerated?`<b>Agreement document</b><br><a href="/documents/sample-agreement.pdf" target="_blank">Open sample agreement PDF</a>`:'';
                    agreementDetails.style.display=agreementGenerated?'block':'none';
                    agreementDetails.innerHTML=agreementGenerated?agreementHtml(status):'';
                    let msg='Start an application.';
                    let cls='note';
                    if(status==='INITIATED'||status==='IN_PROGRESS'||status==='KYC'||status==='IDV') msg='Next: submit the prefilled current step. The fields are mock demo values and can be edited.';
                    else if(status==='READY_FOR_REVIEW'&&!checksDone) msg='Next: run mocked external checks. Results are deterministic based on the scenario key.';
                    else if(status==='READY_FOR_REVIEW') msg='Next: submit for decision.';
                    else if(status==='MANUAL_REVIEW'){msg='Ikano needs to review your application. Policy reason: '+primaryPolicyReason()+'. For demo, use the operations panel to approve or decline.'; cls='note warn';}
                    else if(status==='DECLINED'){msg='Application declined. Policy reason: '+primaryPolicyReason()+'. This is terminal; no further customer action is required.'; cls='note err';}
                    else if(status==='AGREEMENT_CREATED'&&!agreementGenerated) msg='Approved by decisioning. Next: continue to agreement. This generates the sample agreement PDF.';
                    else if(status==='AGREEMENT_CREATED') msg='Agreement PDF generated. Review the sample document, then sign now or choose sign later.';
                    else if(status==='SIGNING_PENDING'){msg='Customer selected sign later. Return later and choose Resume signing, then run account setup.'; cls='note warn';}
                    else if(status==='AGREEMENT_SIGNED') msg='Agreement signed. Next: account setup.';
                    else if(status==='APPROVED'){msg='Onboarding completed successfully. Final state is APPROVED.'; cls='note ok';}
                    actionNote.className=cls; actionNote.textContent=msg;
                  }
                  function agreementHtml(status){
                    const amount=app.customerType==='BUSINESS'?'250,000 SEK':'75,000 SEK';
                    const term=app.customerType==='BUSINESS'?'60 months':'36 months';
                    const apr=app.customerType==='BUSINESS'?'8.9%':'6.9%';
                    const applicant=app.customerType==='BUSINESS'?'Demo Trading AB':'Alex Demo';
                    const disabled=(status==='AGREEMENT_SIGNED'||status==='APPROVED')?'disabled':'';
                    const checked=signConfirmed?'checked':'';
                    return `<h3>Loan agreement preview</h3>
                      <p><b>Agreement reference:</b> agreement-${app.id}</p>
                      <p><b>Applicant:</b> ${applicant}</p>
                      <p><b>Country:</b> ${app.country}</p>
                      <p><b>Product:</b> Ikano onboarding sample account</p>
                      <table style="width:100%;border-collapse:collapse">
                        <tr><td><b>Credit amount</b></td><td>${amount}</td></tr>
                        <tr><td><b>Term</b></td><td>${term}</td></tr>
                        <tr><td><b>Representative APR</b></td><td>${apr}</td></tr>
                        <tr><td><b>Monthly repayment</b></td><td>Calculated in real product platform</td></tr>
                        <tr><td><b>Fees</b></td><td>No real fees in this demo</td></tr>
                      </table>
                      <p>This sample agreement is generated from the approved onboarding application and deterministic mock decisioning. It is not a real banking agreement.</p>
                      <label style="display:flex;gap:.5rem;align-items:flex-start;margin-top:1rem">
                        <input id="signConfirm" type="checkbox" ${checked} ${disabled} onchange="signConfirmed=this.checked;updateButtons()" style="min-width:auto;margin-top:.2rem"/>
                        <span>I have read the agreement, accept the terms, and want to sign electronically using click-to-sign.</span>
                      </label>`;
                  }
                  function policyNotes(){
                    return lastChecks.map(c=>({area:policyArea(c.integrationType), outcome:c.outcome, reasonCode:c.reasonCode, note:c.message}));
                  }
                  function policyArea(type){
                    if(['credit'].includes(type)) return 'Underwriting';
                    if(['sanctions','pep'].includes(type)) return 'AML';
                    if(['identity','bank-account'].includes(type)) return 'Fraud/KYC';
                    if(['registry'].includes(type)) return 'KYB';
                    if(['address'].includes(type)) return 'Address verification';
                    return type;
                  }
                  function primaryPolicyReason(){
                    const issue=lastChecks.find(c=>c.outcome==='FAIL'||c.outcome==='MANUAL_REVIEW'||c.outcome==='UNAVAILABLE');
                    return issue?`${policyArea(issue.integrationType)} - ${issue.reasonCode}: ${issue.message}`:'all Ikano policy checks passed';
                  }
                  function policySummaryHtml(){
                    if(!lastChecks.length) return '';
                    return '<hr/><p><b>Policy notes</b></p><ul>'+policyNotes().map(n=>`<li>${n.area}: ${n.outcome} / ${n.reasonCode} - ${n.note}</li>`).join('')+'</ul>';
                  }
                  init();
                </script>
                </body>
                </html>
                """;
    }
}
