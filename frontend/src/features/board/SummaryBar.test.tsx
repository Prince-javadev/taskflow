import { render, screen, within } from '@testing-library/react';
import { SummaryBar } from './SummaryBar';

describe('SummaryBar', () => {
  it('shows a count per status and the overdue count', () => {
    render(
      <SummaryBar summary={{ projectId: 1, counts: { TODO: 2, IN_PROGRESS: 1, DONE: 4 }, total: 7, overdue: 3 }} />,
    );

    const bar = screen.getByLabelText('Task summary');
    const valueFor = (label: string) => within(bar).getByText(label).previousSibling?.textContent;

    expect(valueFor('To Do')).toBe('2');
    expect(valueFor('In Progress')).toBe('1');
    expect(valueFor('Done')).toBe('4');
    expect(valueFor('Overdue')).toBe('3');
  });
});
