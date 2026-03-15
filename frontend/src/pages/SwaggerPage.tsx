import React from 'react';

const SwaggerPage: React.FC = () => {
  return (
    <iframe
      title="API Docs"
      src="/swagger-ui/index.html"
      style={{ width: '100vw', height: '100vh', border: 'none' }}
    />
  );
};

export default SwaggerPage;
