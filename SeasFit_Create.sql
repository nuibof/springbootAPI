USE [master]
GO
IF DB_ID('SeasFit') IS NOT NULL
    DROP DATABASE [SeasFit]
GO
CREATE DATABASE [SeasFit]
GO
USE [SeasFit]
GO

-- Toàn bộ các CREATE TABLE như user đã paste, sẽ được giữ nguyên ở đây
CREATE TABLE [dbo].[banner](
	[image_url] [varchar](255) NULL,
	[big_text] [nvarchar](100) NULL,
	[sub_text] [nvarchar](100) NULL,
	[button_text] [nvarchar](50) NULL,
	[id] [bigint] IDENTITY(1,1) NOT NULL,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[cart](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [bigint] NOT NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[cart_item](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[cart_id] [bigint] NOT NULL,
	[quantity] [int] NOT NULL,
	[variant_id] [bigint] NULL,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[category](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](100) NOT NULL,
	[description] [text] NULL,
	[image_url] [varchar](255) NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[color](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](50) NOT NULL,
	[hex_code] [varchar](7) NOT NULL,
	[image_url] [varchar](255) NULL,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[favorite](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [bigint] NOT NULL,
	[product_id] [bigint] NOT NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED ([id] ASC),
CONSTRAINT [uq_favorite_user_product] UNIQUE ([user_id], [product_id])
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[inventory](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[product_id] [bigint] NOT NULL,
	[quantity] [int] NOT NULL,
	[updated_at] [datetime] NULL,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[order](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [bigint] NOT NULL,
	[total_amount] [decimal](12, 2) NULL,
	[status] [varchar](20) NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[order_item](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[order_id] [bigint] NOT NULL,
	[quantity] [int] NOT NULL,
	[price] [decimal](12, 2) NOT NULL,
	[variant_id] [bigint] NULL,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[payment](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[order_id] [bigint] NOT NULL,
	[method] [varchar](50) NULL,
	[status] [varchar](20) NULL,
	[transaction_code] [varchar](100) NULL,
	[paid_at] [datetime] NULL,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[product](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[name] [nvarchar](100) NOT NULL,
	[description] [nvarchar](255) NULL,
	[status] [varchar](20) NULL,
	[category_id] [int] NULL,
	[gender] [int] NULL,
	[image_url] [varchar](255) NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[product_image](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[product_id] [bigint] NOT NULL,
	[color_id] [int] NOT NULL,
	[image_url] [varchar](255) NOT NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[product_variant](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[product_id] [bigint] NOT NULL,
	[color_id] [int] NOT NULL,
	[size_id] [int] NOT NULL,
	[quantity] [int] NULL,
	[created_at] [datetime] NULL,
	[price] [decimal](12, 2) NULL,
PRIMARY KEY CLUSTERED ([id] ASC),
CONSTRAINT [UQ_product_color_size] UNIQUE ([product_id], [color_id], [size_id])
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[review](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [bigint] NOT NULL,
	[product_id] [bigint] NOT NULL,
	[rating] [int] NULL CHECK ([rating] >= 1 AND [rating] <= 5),
	[comment] [text] NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[size](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[label] [nvarchar](10) NOT NULL UNIQUE,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
CREATE TABLE [dbo].[user](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_name] [varchar](50) NOT NULL UNIQUE,
	[full_name] [nvarchar](100) NULL,
	[password] [varchar](255) NOT NULL,
	[gender] [bit] NULL,
	[identity_card] [varchar](20) NULL,
	[email] [varchar](100) NULL,
	[phone] [varchar](20) NULL,
	[address] [nvarchar](255) NULL,
	[image_url] [varchar](255) NULL,
	[date_of_birth] [datetime] NULL,
	[status] [varchar](20) NULL,
	[role] [varchar](50) NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED ([id] ASC)
) ON [PRIMARY]
GO
-- Foreign keys
ALTER TABLE [dbo].[cart] ADD FOREIGN KEY ([user_id]) REFERENCES [dbo].[user]([id])
GO
ALTER TABLE [dbo].[cart_item] ADD FOREIGN KEY ([cart_id]) REFERENCES [dbo].[cart]([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[cart_item] ADD FOREIGN KEY ([variant_id]) REFERENCES [dbo].[product_variant]([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[favorite] ADD FOREIGN KEY ([user_id]) REFERENCES [dbo].[user]([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[favorite] ADD FOREIGN KEY ([product_id]) REFERENCES [dbo].[product]([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[inventory] ADD FOREIGN KEY ([product_id]) REFERENCES [dbo].[product]([id])
GO
ALTER TABLE [dbo].[order] ADD FOREIGN KEY ([user_id]) REFERENCES [dbo].[user]([id])
GO
ALTER TABLE [dbo].[order_item] ADD FOREIGN KEY ([order_id]) REFERENCES [dbo].[order]([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[order_item] ADD FOREIGN KEY ([variant_id]) REFERENCES [dbo].[product_variant]([id])
GO
ALTER TABLE [dbo].[payment] ADD FOREIGN KEY ([order_id]) REFERENCES [dbo].[order]([id])
GO
ALTER TABLE [dbo].[product] ADD FOREIGN KEY ([category_id]) REFERENCES [dbo].[category]([id])
GO
ALTER TABLE [dbo].[product_image] ADD FOREIGN KEY ([product_id]) REFERENCES [dbo].[product]([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[product_image] ADD FOREIGN KEY ([color_id]) REFERENCES [dbo].[color]([id])
GO
ALTER TABLE [dbo].[product_variant] ADD FOREIGN KEY ([product_id]) REFERENCES [dbo].[product]([id]) ON DELETE CASCADE
GO
ALTER TABLE [dbo].[product_variant] ADD FOREIGN KEY ([color_id]) REFERENCES [dbo].[color]([id])
GO
ALTER TABLE [dbo].[product_variant] ADD FOREIGN KEY ([size_id]) REFERENCES [dbo].[size]([id])
GO
ALTER TABLE [dbo].[review] ADD FOREIGN KEY ([user_id]) REFERENCES [dbo].[user]([id])
GO
ALTER TABLE [dbo].[review] ADD FOREIGN KEY ([product_id]) REFERENCES [dbo].[product]([id])
GO
