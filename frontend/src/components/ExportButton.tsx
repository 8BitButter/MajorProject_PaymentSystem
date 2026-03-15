import React from 'react';
import { Button } from '@mui/material';

interface ExportButtonProps {
    data: any[];
    filename?: string;
    label?: string;
}

const ExportButton: React.FC<ExportButtonProps> = ({ data, filename = 'export.xlsx', label = 'Export CSV' }) => {
    const handleExport = async () => {
        const { utils, writeFile } = await import('xlsx');
        const ws = utils.json_to_sheet(data);
        const wb = utils.book_new();
        utils.book_append_sheet(wb, ws, 'Sheet1');
        writeFile(wb, filename);
    };
    return <Button onClick={handleExport} variant="outlined" size="small">{label}</Button>;
};

export default ExportButton;