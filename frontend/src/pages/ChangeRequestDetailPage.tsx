import { useEffect, useState, type ReactNode } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  getChangeRequest, getApprovals, getComments, getAuditLogs,
  getPolicyViolations, getRiskAssessment,
} from '../api/changeRequests';
import type {
  ChangeRequestDetailResponse, RiskAssessmentResponse, PolicyViolationResponse,
  ApprovalResponse, AuditLogResponse, CommentResponse, HealthCheckResponse,
  CheckType, HealthCheckItem, HealthResult, Decision,
} from '../types/api';
import { getErrorMessage } from '../lib/errorMessages';
import { formatDateTime } from '../lib/dateUtils';
import RiskBadge from '../components/RiskBadge';
import EnvBadge from '../components/EnvBadge';
import StatusBadge from '../components/StatusBadge';

const CHECK_TYPE_LABEL: Record<CheckType, string> = {
  BACKUP: 'バックアップ', ROLLBACK: '切戻し', MONITORING: '監視',
  IMPACT: '影響範囲', STAKEHOLDER: '関係者周知', WINDOW: '実施時間帯', APPROVAL: '承認',
};
const HEALTH_ITEM_LABEL: Record<HealthCheckItem, string> = {
  IAC_APPLY: 'IaC 適用', ALB_TARGET_HEALTH: 'ALB ターゲット', EC2_SSM: 'EC2 / SSM',
  HTTP_HEALTH: 'HTTP ヘルス', CW_ALARM: 'CloudWatch アラーム', APP_REACHABILITY: 'アプリ到達性',
  DB_CONNECTION: 'DB 接続', NOTE: '備考',
};
const HEALTH_RESULT: Record<HealthResult, { label: string; cls: string }> = {
  HEALTHY: { label: '正常', cls: 'bg-green-100 text-green-800 border-green-300' },
  WARNING: { label: '警告', cls: 'bg-amber-100 text-amber-800 border-amber-300' },
  UNHEALTHY: { label: '異常', cls: 'bg-red-100 text-red-800 border-red-400' },
  NOT_CHECKED: { label: '未確認', cls: 'bg-gray-100 text-gray-600 border-gray-300' },
};
const DECISION: Record<Decision, { label: string; cls: string }> = {
  APPROVED: { label: '承認', cls: 'bg-teal-100 text-teal-800 border-teal-300' },
  REJECTED: { label: '却下', cls: 'bg-rose-100 text-rose-800 border-rose-300' },
  RETURNED: { label: '差戻し', cls: 'bg-amber-100 text-amber-800 border-amber-300' },
};

const userLabel = (id: number | null) => (id == null ? '—' : `ユーザー #${id}`);

export default function ChangeRequestDetailPage() {
  const { id } = useParams();
  const crId = Number(id);
  const [detail, setDetail] = useState<ChangeRequestDetailResponse | null>(null);
  const [risk, setRisk] = useState<RiskAssessmentResponse | null>(null);
  const [policies, setPolicies] = useState<PolicyViolationResponse[]>([]);
  const [approvals, setApprovals] = useState<ApprovalResponse[]>([]);
  const [audits, setAudits] = useState<AuditLogResponse[]>([]);
  const [comments, setComments] = useState<CommentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openFinding, setOpenFinding] = useState<Record<number, boolean>>({});

  useEffect(() => {
    if (!Number.isInteger(crId)) { setError('変更申請が見つかりません'); setLoading(false); return; }
    let active = true;
    setLoading(true);
    Promise.all([
      getChangeRequest(crId), getRiskAssessment(crId), getPolicyViolations(crId),
      getApprovals(crId), getAuditLogs(crId), getComments(crId),
    ])
      .then(([d, r, p, a, au, c]) => {
        if (!active) return;
        setDetail(d); setRisk(r); setPolicies(p); setApprovals(a); setAudits(au); setComments(c);
        setError(null);
      })
      .catch((e) => { if (active) setError(getErrorMessage(e, '変更申請が見つかりません')); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [crId]);

  if (loading) return <div className="text-gray-500">読み込み中…</div>;
  if (error || !detail) {
    return (
      <div>
        <div role="alert" className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error ?? '変更申請が見つかりません'}</div>
        <Link to="/change-requests" className="mt-3 inline-block text-sm text-blue-600 hover:underline">← 一覧へ戻る</Link>
      </div>
    );
  }

  const cr = detail.changeRequest;
  const iacApply = detail.healthChecks.filter((h) => h.checkItem === 'IAC_APPLY');
  const otherHealth = detail.healthChecks.filter((h) => h.checkItem !== 'IAC_APPLY');

  return (
    <div className="space-y-6">
      <Link to="/change-requests" className="text-sm text-blue-600 hover:underline">← 一覧へ戻る</Link>

      <header className={`rounded-lg border p-4 ${cr.targetEnvironment === 'production' ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white'}`}>
        <div className="mb-2 flex flex-wrap items-center gap-2">
          {cr.targetEnvironment === 'production' && <span className="rounded bg-red-600 px-1.5 py-0.5 text-xs font-bold text-white">本番</span>}
          <h1 className="text-xl font-bold text-gray-800">{cr.title}</h1>
        </div>
        <div className="flex flex-wrap items-center gap-2 text-sm">
          <EnvBadge env={cr.targetEnvironment} />
          <RiskBadge level={cr.riskLevel} />
          <StatusBadge status={cr.status} />
          <span className="text-gray-500">ID #{cr.id}</span>
        </div>
        <dl className="mt-3 grid grid-cols-2 gap-x-6 gap-y-1 text-sm text-gray-700">
          <Field label="IaC 種別" value={cr.iacType} />
          <Field label="申請者" value={userLabel(cr.requesterId)} />
          <Field label="実施予定" value={formatDateTime(cr.scheduledAt)} />
          <Field label="作成" value={formatDateTime(cr.createdAt)} />
          <Field label="更新" value={formatDateTime(cr.updatedAt)} />
        </dl>
      </header>

      <Section title="概要">
        <dl className="grid grid-cols-1 gap-y-2 text-sm text-gray-700">
          <Field label="対象サービス" value={cr.targetAwsService ?? '—'} />
          <Field label="対象リソース名" value={cr.targetResourceName ?? '—'} />
          <Field label="変更理由" value={cr.changeReason ?? '—'} />
          <Field label="変更概要" value={cr.changeSummary ?? '—'} />
          <Field label="切戻し手順" value={cr.rollbackProcedure ?? '—'} />
        </dl>
      </Section>

      <Section title="差分">
        {cr.diffText ? (
          <pre className="overflow-x-auto rounded bg-gray-900 p-3 text-xs text-gray-100">{cr.diffText}</pre>
        ) : <p className="text-sm text-gray-500">差分は未入力です。</p>}
      </Section>

      <Section title="リスク判定">
        {!risk ? (
          <p className="text-sm text-gray-500">まだリスク判定は行われていません。</p>
        ) : (
          <div>
            <div className="mb-2 flex flex-wrap items-center gap-2">
              <RiskBadge level={risk.riskLevel} />
              {risk.blocked && <span className="rounded bg-red-600 px-1.5 py-0.5 text-xs font-bold text-white">ブロック</span>}
              {risk.requiresAdditionalApproval && <span className="rounded bg-amber-100 px-1.5 py-0.5 text-xs font-bold text-amber-800">追加承認</span>}
              <span className="text-xs text-gray-500">{formatDateTime(risk.assessedAt)}</span>
            </div>
            {risk.findings.length === 0 ? (
              <p className="text-sm text-gray-500">検知された注意点はありません。</p>
            ) : (
              <ul className="space-y-2">
                {risk.findings.map((fd, i) => (
                  <li key={fd.ruleCode} className={`rounded border p-3 ${fd.isBlock ? 'border-red-300 bg-red-50' : 'border-gray-200'}`}>
                    <div className="flex items-center gap-2">
                      <RiskBadge level={fd.riskLevel} />
                      <span className="font-medium text-gray-800">{fd.ruleName}</span>
                      {fd.isBlock && <span className="rounded bg-red-600 px-1.5 py-0.5 text-xs font-bold text-white">ブロック</span>}
                      <button type="button" onClick={() => setOpenFinding((p) => ({ ...p, [i]: !p[i] }))} className="ml-auto text-sm text-blue-600 hover:underline">{openFinding[i] ? '閉じる' : '詳しく'}</button>
                    </div>
                    {openFinding[i] && (
                      <dl className="mt-2 space-y-1 text-sm text-gray-700">
                        <Field label="なぜ危険か" value={fd.whyDangerous} />
                        <Field label="想定される影響" value={fd.expectedImpact} />
                        <Field label="推奨対応" value={fd.recommendedAction} />
                      </dl>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </Section>

      <Section title="ポリシー違反">
        {policies.length === 0 ? (
          <p className="text-sm text-gray-500">ポリシー違反はありません。</p>
        ) : (
          <ul className="space-y-2 text-sm">
            {policies.map((p, i) => (
              <li key={i} className="rounded border border-amber-300 bg-amber-50 p-2">
                <span className="font-medium text-amber-900">{p.effect}</span>
                <span className="ml-2 text-amber-800">{p.message}</span>
                <span className="ml-2 text-xs text-gray-500">{formatDateTime(p.detectedAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </Section>

      <Section title="承認履歴">
        {approvals.length === 0 ? (
          <p className="text-sm text-gray-500">まだ承認・却下はありません。</p>
        ) : (
          <ul className="space-y-2 text-sm">
            {approvals.map((a) => (
              <li key={a.id} className="flex flex-wrap items-center gap-2 border-b border-gray-100 pb-2">
                <span className={`inline-flex rounded border px-2 py-0.5 text-xs font-bold ${DECISION[a.decision].cls}`}>{DECISION[a.decision].label}</span>
                <span className="text-gray-700">{userLabel(a.reviewerId)}</span>
                {a.comment && <span className="text-gray-600">「{a.comment}」</span>}
                <span className="ml-auto text-xs text-gray-500">{formatDateTime(a.decidedAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </Section>

      <Section title="実施前チェック">
        {detail.preChecks.length === 0 ? (
          <p className="text-sm text-gray-500">実施前チェックはありません。</p>
        ) : (
          <ul className="space-y-1 text-sm">
            {detail.preChecks.map((c) => (
              <li key={c.id} className="flex flex-wrap items-center gap-2">
                <span className={c.completed ? 'text-green-700' : 'text-gray-400'}>{c.completed ? '✓' : '○'}</span>
                <span className="text-gray-700">{CHECK_TYPE_LABEL[c.checkType]}</span>
                {c.required && <span className="rounded bg-gray-200 px-1 text-xs text-gray-600">必須</span>}
                {c.completed && <span className="ml-auto text-xs text-gray-500">{userLabel(c.completedBy)}・{formatDateTime(c.completedAt)}</span>}
              </li>
            ))}
          </ul>
        )}
      </Section>

      <Section title="実施後ヘルスチェック">
        <div className="space-y-3">
          <div>
            <h3 className="mb-1 text-sm font-bold text-gray-600">IaC 適用</h3>
            {detail.execution ? (
              <div className="flex flex-wrap items-center gap-2 text-sm">
                <span className={`inline-flex rounded border px-2 py-0.5 text-xs font-bold ${detail.execution.iacApplyResult === 'SUCCESS' ? HEALTH_RESULT.HEALTHY.cls : HEALTH_RESULT.UNHEALTHY.cls}`}>
                  {detail.execution.iacApplyResult === 'SUCCESS' ? '適用成功' : '適用失敗'}
                </span>
                <span className="text-gray-600">サービス正常性：{detail.execution.serviceHealthConfirmed ? '確認済' : '未確認'}</span>
                {detail.execution.rollbackPerformed && <span className="rounded bg-orange-100 px-1.5 py-0.5 text-xs font-bold text-orange-800">切戻し実施</span>}
              </div>
            ) : iacApply.length > 0 ? (
              <HealthList items={iacApply} />
            ) : <p className="text-sm text-gray-500">実施記録はまだありません。</p>}
          </div>
          <div>
            <h3 className="mb-1 text-sm font-bold text-gray-600">サービス正常性</h3>
            {otherHealth.length === 0 ? (
              <p className="text-sm text-gray-500">記録はありません。</p>
            ) : <HealthList items={otherHealth} />}
          </div>
        </div>
      </Section>

      <Section title="監査ログ">
        {audits.length === 0 ? (
          <p className="text-sm text-gray-500">監査ログはありません。</p>
        ) : (
          <ul className="space-y-1 text-sm">
            {audits.map((a) => (
              <li key={a.id} className="flex flex-wrap items-center gap-2 border-b border-gray-100 pb-1">
                <span className="font-mono text-xs text-gray-700">{a.actionType}</span>
                {(a.beforeStatus || a.afterStatus) && <span className="text-xs text-gray-500">{a.beforeStatus ?? '—'} → {a.afterStatus ?? '—'}</span>}
                {a.summary && <span className="text-gray-600">{a.summary}</span>}
                <span className="ml-auto text-xs text-gray-400">{userLabel(a.actorId)}・{formatDateTime(a.createdAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </Section>

      <Section title="コメント">
        {comments.length === 0 ? (
          <p className="text-sm text-gray-500">コメントはありません。</p>
        ) : (
          <ul className="space-y-2 text-sm">
            {comments.map((c) => (
              <li key={c.id} className="border-b border-gray-100 pb-2">
                <div className="flex items-center gap-2 text-xs text-gray-500">
                  <span>{userLabel(c.authorId)}</span><span>{formatDateTime(c.createdAt)}</span>
                </div>
                <p className="whitespace-pre-wrap text-gray-700">{c.body}</p>
              </li>
            ))}
          </ul>
        )}
      </Section>
    </div>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-gray-200 bg-white p-4">
      <h2 className="mb-2 text-lg font-bold text-gray-800">{title}</h2>
      {children}
    </section>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs font-medium text-gray-500">{label}</dt>
      <dd className="whitespace-pre-wrap text-gray-800">{value}</dd>
    </div>
  );
}

function HealthList({ items }: { items: HealthCheckResponse[] }) {
  return (
    <ul className="space-y-1 text-sm">
      {items.map((h) => (
        <li key={h.id} className="flex flex-wrap items-center gap-2">
          <span className="text-gray-700">{HEALTH_ITEM_LABEL[h.checkItem]}</span>
          <span className={`inline-flex rounded border px-2 py-0.5 text-xs font-bold ${HEALTH_RESULT[h.result].cls}`}>{HEALTH_RESULT[h.result].label}</span>
          {h.note && <span className="text-gray-600">{h.note}</span>}
          <span className="ml-auto text-xs text-gray-400">{userLabel(h.recordedBy)}・{formatDateTime(h.recordedAt)}</span>
        </li>
      ))}
    </ul>
  );
}
