import { useState, type ChangeEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { createChangeRequest, previewRisk, submitChangeRequest } from '../api/changeRequests';
import type { CreateChangeRequest, Environment, IacType, PreviewRiskResponse } from '../types/api';
import { getErrorMessage } from '../lib/errorMessages';
import RiskBadge from '../components/RiskBadge';

type FormState = {
  title: string;
  targetEnvironment: Environment;
  iacType: IacType;
  targetAwsService: string;
  targetResourceName: string;
  changeReason: string;
  changeSummary: string;
  diffText: string;
  scheduledAt: string;
  rollbackProcedure: string;
};

const INITIAL: FormState = {
  title: '',
  targetEnvironment: 'development',
  iacType: 'TERRAFORM',
  targetAwsService: '',
  targetResourceName: '',
  changeReason: '',
  changeSummary: '',
  diffText: '',
  scheduledAt: '',
  rollbackProcedure: '',
};

const LABELS: Record<keyof FormState, string> = {
  title: 'タイトル',
  targetEnvironment: '対象環境',
  iacType: 'IaC 種別',
  targetAwsService: '対象サービス',
  targetResourceName: '対象リソース名',
  changeReason: '変更理由',
  changeSummary: '変更概要',
  diffText: '差分',
  scheduledAt: '実施予定日時',
  rollbackProcedure: '切戻し手順',
};

const ENVS: Environment[] = ['development', 'staging', 'production'];
const IAC_TYPES: IacType[] = ['TERRAFORM', 'CLOUDFORMATION'];

const REQUIRED_DRAFT: (keyof FormState)[] = ['title', 'targetEnvironment', 'iacType'];
const REQUIRED_SUBMIT: (keyof FormState)[] = [
  ...REQUIRED_DRAFT,
  'targetAwsService',
  'targetResourceName',
  'changeReason',
  'changeSummary',
  'diffText',
];

const KIND_LABEL: Record<'RISK' | 'POLICY', string> = { RISK: 'リスク', POLICY: 'ポリシー' };

function buildBody(f: FormState): CreateChangeRequest {
  const opt = (s: string) => {
    const v = s.trim();
    return v ? v : undefined;
  };
  let scheduledAt: string | undefined;
  if (f.scheduledAt) {
    const d = new Date(f.scheduledAt);
    if (!Number.isNaN(d.getTime())) scheduledAt = d.toISOString();
  }
  return {
    title: f.title.trim(),
    targetEnvironment: f.targetEnvironment,
    iacType: f.iacType,
    targetAwsService: opt(f.targetAwsService),
    targetResourceName: opt(f.targetResourceName),
    changeReason: opt(f.changeReason),
    changeSummary: opt(f.changeSummary),
    diffText: opt(f.diffText),
    scheduledAt,
    rollbackProcedure: opt(f.rollbackProcedure),
  };
}

function validate(f: FormState, keys: (keyof FormState)[]): Record<string, string> {
  const errs: Record<string, string> = {};
  for (const k of keys) {
    if (!String(f[k]).trim()) errs[k] = `${LABELS[k]}は必須です`;
  }
  return errs;
}

export default function ChangeRequestNewPage() {
  const navigate = useNavigate();
  const [f, setF] = useState<FormState>(INITIAL);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [preview, setPreview] = useState<PreviewRiskResponse | null>(null);
  const [expanded, setExpanded] = useState<Record<number, boolean>>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const set =
    (k: keyof FormState) =>
    (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
      setF((prev) => ({ ...prev, [k]: e.target.value }));

  const onPreview = async () => {
    const errs = validate(f, REQUIRED_DRAFT);
    setFieldErrors(errs);
    if (Object.keys(errs).length) return;
    setBusy(true);
    setError(null);
    try {
      setPreview(await previewRisk(buildBody(f)));
      setExpanded({});
    } catch (e) {
      setError(getErrorMessage(e, 'リスクのプレビューに失敗しました'));
    } finally {
      setBusy(false);
    }
  };

  const onSaveDraft = async () => {
    const errs = validate(f, REQUIRED_DRAFT);
    setFieldErrors(errs);
    if (Object.keys(errs).length) return;
    setBusy(true);
    setError(null);
    try {
      await createChangeRequest(buildBody(f));
      navigate('/change-requests');
    } catch (e) {
      setError(getErrorMessage(e, '下書きの保存に失敗しました'));
    } finally {
      setBusy(false);
    }
  };

  const onSaveAndSubmit = async () => {
    const errs = validate(f, REQUIRED_SUBMIT);
    setFieldErrors(errs);
    if (Object.keys(errs).length) {
      setError('提出にはすべての必須項目の入力が必要です');
      return;
    }
    if (preview?.blocked) {
      setError('ポリシー違反のため提出できません。下記の理由を解消してください');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const created = await createChangeRequest(buildBody(f));
      await submitChangeRequest(created.id);
      navigate('/change-requests');
    } catch (e) {
      setError(getErrorMessage(e, '提出に失敗しました'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="max-w-3xl">
      <h1 className="mb-4 text-2xl font-bold text-gray-800">変更申請の作成</h1>
      {error && <div role="alert" className="mb-4 rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}

      <div className="space-y-4 rounded-lg border border-gray-200 bg-white p-5">
        <Text id="title" label={LABELS.title} required value={f.title} error={fieldErrors.title} onChange={set('title')} />
        <div className="flex flex-wrap gap-4">
          <SelectField id="targetEnvironment" label={LABELS.targetEnvironment} required value={f.targetEnvironment} options={ENVS} onChange={set('targetEnvironment')} />
          <SelectField id="iacType" label={LABELS.iacType} required value={f.iacType} options={IAC_TYPES} onChange={set('iacType')} />
        </div>
        <Text id="targetAwsService" label={LABELS.targetAwsService} hint="提出時必須" value={f.targetAwsService} error={fieldErrors.targetAwsService} onChange={set('targetAwsService')} />
        <Text id="targetResourceName" label={LABELS.targetResourceName} hint="提出時必須" value={f.targetResourceName} error={fieldErrors.targetResourceName} onChange={set('targetResourceName')} />
        <Area id="changeReason" label={LABELS.changeReason} hint="提出時必須" value={f.changeReason} error={fieldErrors.changeReason} onChange={set('changeReason')} />
        <Area id="changeSummary" label={LABELS.changeSummary} hint="提出時必須" value={f.changeSummary} error={fieldErrors.changeSummary} onChange={set('changeSummary')} />
        <Area id="diffText" label={LABELS.diffText} hint="提出時必須" mono value={f.diffText} error={fieldErrors.diffText} onChange={set('diffText')} />
        <div className="flex flex-col">
          <label htmlFor="scheduledAt" className="mb-1 text-sm font-medium text-gray-700">{LABELS.scheduledAt}</label>
          <input id="scheduledAt" type="datetime-local" value={f.scheduledAt} onChange={set('scheduledAt')} className="rounded border border-gray-300 px-3 py-2 text-sm" />
        </div>
        <Area id="rollbackProcedure" label={LABELS.rollbackProcedure} value={f.rollbackProcedure} onChange={set('rollbackProcedure')} />
      </div>

      <div className="mt-4 flex flex-wrap gap-3">
        <button type="button" disabled={busy} onClick={onPreview} className="rounded border border-blue-600 px-4 py-2 text-sm font-bold text-blue-700 hover:bg-blue-50 disabled:opacity-40">リスクをプレビュー</button>
        <button type="button" disabled={busy} onClick={onSaveDraft} className="rounded border border-gray-400 px-4 py-2 text-sm font-bold text-gray-700 hover:bg-gray-50 disabled:opacity-40">下書き保存</button>
        <button type="button" disabled={busy || preview?.blocked} onClick={onSaveAndSubmit} className="rounded bg-blue-600 px-4 py-2 text-sm font-bold text-white hover:bg-blue-700 disabled:opacity-40">保存して提出</button>
        <button type="button" disabled={busy} onClick={() => navigate('/change-requests')} className="rounded px-4 py-2 text-sm text-gray-500 hover:bg-gray-100">キャンセル</button>
      </div>

      {preview && (
        <section className="mt-6 rounded-lg border border-gray-200 bg-white p-5">
          <div className="mb-3 flex items-center gap-2">
            <h2 className="text-lg font-bold text-gray-800">リスク判定（プレビュー）</h2>
            <RiskBadge level={preview.riskLevel} />
          </div>
          {preview.blocked ? (
            <div className="mb-4 rounded border border-red-300 bg-red-50 p-3">
              <p className="font-bold text-red-700">この内容では提出できません</p>
              <ul className="mt-2 space-y-1 text-sm text-red-700">
                {preview.blockReasons.map((b, i) => (
                  <li key={i}>
                    <span className="mr-1 rounded bg-red-600 px-1.5 py-0.5 text-xs font-bold text-white">{KIND_LABEL[b.kind]}</span>
                    {b.message}
                    {b.recommendedAction && <span className="block pl-1 text-red-600">推奨対応：{b.recommendedAction}</span>}
                  </li>
                ))}
              </ul>
            </div>
          ) : preview.requiresAdditionalApproval ? (
            <div className="mb-4 rounded border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">この内容は追加の承認が必要になります。</div>
          ) : null}
          {preview.findings.length === 0 ? (
            <p className="text-sm text-gray-500">検知された注意点はありません。</p>
          ) : (
            <ul className="space-y-2">
              {preview.findings.map((fd, i) => (
                <li key={fd.ruleCode} className={`rounded border p-3 ${fd.isBlock ? 'border-red-300 bg-red-50' : 'border-gray-200'}`}>
                  <div className="flex items-center gap-2">
                    <RiskBadge level={fd.riskLevel} />
                    <span className="font-medium text-gray-800">{fd.ruleName}</span>
                    {fd.isBlock && <span className="rounded bg-red-600 px-1.5 py-0.5 text-xs font-bold text-white">ブロック</span>}
                    <button type="button" onClick={() => setExpanded((p) => ({ ...p, [i]: !p[i] }))} className="ml-auto text-sm text-blue-600 hover:underline">{expanded[i] ? '閉じる' : '詳しく'}</button>
                  </div>
                  {expanded[i] && (
                    <dl className="mt-2 space-y-1 text-sm text-gray-700">
                      <Detail term="なぜ危険か" desc={fd.whyDangerous} />
                      <Detail term="想定される影響" desc={fd.expectedImpact} />
                      <Detail term="推奨対応" desc={fd.recommendedAction} />
                    </dl>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  );
}

function Text({ id, label, value, onChange, required, hint, error }: { id: string; label: string; value: string; onChange: (e: ChangeEvent<HTMLInputElement>) => void; required?: boolean; hint?: string; error?: string }) {
  return (
    <div className="flex flex-col">
      <FieldLabel label={label} required={required} hint={hint} htmlFor={id} />
      <input id={id} value={value} onChange={onChange} className={`rounded border px-3 py-2 text-sm ${error ? 'border-red-400' : 'border-gray-300'}`} />
      {error && <span className="mt-1 text-xs text-red-600">{error}</span>}
    </div>
  );
}

function Area({ id, label, value, onChange, required, hint, error, mono }: { id: string; label: string; value: string; onChange: (e: ChangeEvent<HTMLTextAreaElement>) => void; required?: boolean; hint?: string; error?: string; mono?: boolean }) {
  return (
    <div className="flex flex-col">
      <FieldLabel label={label} required={required} hint={hint} htmlFor={id} />
      <textarea id={id} value={value} onChange={onChange} rows={mono ? 6 : 3} className={`rounded border px-3 py-2 text-sm ${mono ? 'font-mono' : ''} ${error ? 'border-red-400' : 'border-gray-300'}`} />
      {error && <span className="mt-1 text-xs text-red-600">{error}</span>}
    </div>
  );
}

function SelectField({ id, label, value, options, onChange, required }: { id: string; label: string; value: string; options: string[]; onChange: (e: ChangeEvent<HTMLSelectElement>) => void; required?: boolean }) {
  return (
    <div className="flex flex-col">
      <FieldLabel label={label} required={required} htmlFor={id} />
      <select id={id} value={value} onChange={onChange} className="rounded border border-gray-300 px-3 py-2 text-sm">
        {options.map((o) => <option key={o} value={o}>{o}</option>)}
      </select>
    </div>
  );
}

function FieldLabel({ label, required, hint, htmlFor }: { label: string; required?: boolean; hint?: string; htmlFor?: string }) {
  return (
    <label htmlFor={htmlFor} className="mb-1 text-sm font-medium text-gray-700">
      {label}
      {required && <span className="ml-1 text-red-600">*</span>}
      {hint && <span className="ml-2 text-xs font-normal text-gray-400">{hint}</span>}
    </label>
  );
}

function Detail({ term, desc }: { term: string; desc: string }) {
  return (
    <div>
      <dt className="font-medium text-gray-600">{term}</dt>
      <dd className="pl-2">{desc}</dd>
    </div>
  );
}
